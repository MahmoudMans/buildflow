"use client";

import { Fragment, useId, useState } from "react";

import { ResponsiveTable } from "@/components/ux/ResponsiveTable";
import {
  actionTone,
  actorDetail,
  actorLabel,
  entrySubject,
  formatAuditTime,
  humanizeAction,
  humanizeEntityType,
  parseAuditValues,
} from "@/lib/audit-log";
import type { AuditValues } from "@/lib/audit-log";
import type { AuditEntry } from "@/lib/audit-types";

const REDACTED = "[redacted]";

function Values({ title, values }: { title: string; values: AuditValues }) {
  if (values.kind === "none") {
    return null;
  }

  return (
    <section className="uxAuditValues">
      <h4>{title}</h4>

      {values.kind === "text" && <pre className="uxAuditRaw">{values.text}</pre>}

      {values.kind === "fields" && values.fields.length === 0 && (
        <p className="uxAuditSub">Nothing was recorded.</p>
      )}

      {values.kind === "fields" && values.fields.length > 0 && (
        <dl className="uxAuditFields">
          {values.fields.map((item, index) => (
            <Fragment key={`${item.key}-${index}`}>
              <dt>{item.label}</dt>

              {item.value === REDACTED ? (
                <dd className="uxAuditRedacted">Hidden for security</dd>
              ) : (
                <dd>{item.value}</dd>
              )}
            </Fragment>
          ))}
        </dl>
      )}
    </section>
  );
}

function Details({ entry, timeZone }: { entry: AuditEntry; timeZone: string }) {
  const newValues = parseAuditValues(entry.newValues);
  const oldValues = parseAuditValues(entry.oldValues);
  const time = formatAuditTime(entry.createdAt, timeZone);

  return (
    <div className="uxAuditDetails">
      {newValues.kind === "none" && oldValues.kind === "none" ? (
        <p className="uxAuditSub">No values were recorded for this action.</p>
      ) : (
        <>
          <Values title={oldValues.kind === "none" ? "Recorded values" : "After"} values={newValues} />

          <Values title="Before" values={oldValues} />
        </>
      )}

      <dl className="uxAuditMeta">
        <dt>Time</dt>
        <dd>
          {time.label} ({timeZone})
        </dd>

        <dt>Entry ID</dt>
        <dd>
          <code>{entry.id}</code>
        </dd>

        {entry.entityId && (
          <>
            <dt>Record ID</dt>
            <dd>
              <code>{entry.entityId}</code>
            </dd>
          </>
        )}

        {entry.ipAddress && (
          <>
            <dt>IP address</dt>
            <dd>{entry.ipAddress}</dd>
          </>
        )}
      </dl>
    </div>
  );
}

// One row per recorded action, with the values behind each row one click away.
export function AuditLogTable({
  items,
  timeZone,
}: {
  items: AuditEntry[];
  timeZone: string;
}) {
  const id = useId();
  const [open, setOpen] = useState<Record<string, boolean>>({});

  function toggle(entryId: string) {
    setOpen((current) => ({ ...current, [entryId]: !current[entryId] }));
  }

  return (
    <ResponsiveTable label="Audit log entries">
      <table className="uxAuditTable">
        <thead>
          <tr>
            <th scope="col">When</th>
            <th scope="col">Who</th>
            <th scope="col">What</th>
            <th scope="col">Project</th>
            <th scope="col">
              <span className="uxSrOnly">Details</span>
            </th>
          </tr>
        </thead>

        <tbody>
          {items.map((entry) => {
            const time = formatAuditTime(entry.createdAt, timeZone);
            const action = humanizeAction(entry.action);
            const subject = entrySubject(entry);
            const detail = actorDetail(entry);
            const expanded = open[entry.id] === true;
            const panelId = `${id}-${entry.id}`;

            return (
              <Fragment key={entry.id}>
                <tr className={expanded ? "uxAuditRow open" : "uxAuditRow"}>
                  <td data-label="When">
                    <time dateTime={entry.createdAt} title={time.label}>
                      <span className="uxAuditDate">{time.date}</span>
                      <span className="uxAuditSub">{time.time}</span>
                    </time>
                  </td>

                  <td data-label="Who">
                    <strong className="uxAuditWho">{actorLabel(entry)}</strong>
                    {detail && <span className="uxAuditSub">{detail}</span>}
                  </td>

                  <td data-label="What">
                    <span className={`uxStatusBadge uxAuditAction ${actionTone(entry.action)}`}>
                      <span className="uxStatusDot" aria-hidden="true" />
                      {action}
                    </span>

                    <span className="uxAuditSub">
                      {humanizeEntityType(entry.entityType)}
                      {subject ? ` · ${subject}` : ""}
                    </span>
                  </td>

                  <td data-label="Project">
                    {entry.projectName ? (
                      <>
                        <span className="uxAuditDate">{entry.projectName}</span>
                        {entry.projectNumber && (
                          <span className="uxAuditSub">{entry.projectNumber}</span>
                        )}
                      </>
                    ) : (
                      <span className="uxAuditSub">Whole organization</span>
                    )}
                  </td>

                  <td className="uxAuditToggleCell">
                    <button
                      type="button"
                      className="uxLinkButton"
                      aria-expanded={expanded}
                      aria-controls={panelId}
                      aria-label={`Details of ${action} on ${time.label}`}
                      onClick={() => toggle(entry.id)}
                    >
                      <span aria-hidden="true">{expanded ? "▾" : "▸"}</span> Details
                    </button>
                  </td>
                </tr>

                {expanded && (
                  <tr className="uxAuditDetailRow">
                    <td colSpan={5} id={panelId}>
                      <Details entry={entry} timeZone={timeZone} />
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </ResponsiveTable>
  );
}
