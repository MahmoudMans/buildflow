"use client";

import { useRouter } from "next/navigation";
import {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from "react";

import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { ApiClientError, apiGet } from "@/lib/api";
import {
  MIN_SEARCH_LENGTH,
  OPEN_SEARCH_EVENT,
  buildSections,
  statusLabel,
  type SearchHit,
  type SearchResponse,
} from "@/lib/search";

const DEBOUNCE_MS = 220;

type SearchState = "idle" | "loading" | "ready" | "error";

export function CommandPalette() {
  const router = useRouter();
  const { organizations } = useAuth();
  const { organizationId } = useOrganizationSelection(organizations);

  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<SearchHit[]>([]);
  const [state, setState] = useState<SearchState>("idle");
  const [message, setMessage] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);

  const inputRef = useRef<HTMLInputElement>(null);
  const previousFocus = useRef<HTMLElement | null>(null);
  const timer = useRef<number | undefined>(undefined);
  const sequence = useRef(0);
  const listId = useId();

  const openPalette = useCallback(() => {
    previousFocus.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    window.clearTimeout(timer.current);
    sequence.current += 1;

    setQuery("");
    setHits([]);
    setState("idle");
    setMessage("");
    setActiveIndex(0);
    setOpen(true);
  }, []);

  const closePalette = useCallback(() => {
    window.clearTimeout(timer.current);
    sequence.current += 1;

    setOpen(false);

    previousFocus.current?.focus();
  }, []);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (
        (event.metaKey || event.ctrlKey) &&
        !event.altKey &&
        event.key.toLowerCase() === "k"
      ) {
        event.preventDefault();

        if (open) {
          closePalette();
        } else {
          openPalette();
        }
      }
    }

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener(OPEN_SEARCH_EVENT, openPalette);

    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener(OPEN_SEARCH_EVENT, openPalette);
      window.clearTimeout(timer.current);
    };
  }, [open, openPalette, closePalette]);

  useEffect(() => {
    if (open) {
      inputRef.current?.focus();
    }
  }, [open]);

  const runSearch = useCallback(
    async (text: string) => {
      const requestId = sequence.current + 1;

      sequence.current = requestId;

      try {
        const result = await apiGet<SearchResponse>(
          `/search?q=${encodeURIComponent(text)}`,
          organizationId,
        );

        if (sequence.current !== requestId) {
          return;
        }

        setHits(result.hits);
        setState("ready");
      } catch (caught) {
        if (sequence.current !== requestId) {
          return;
        }

        setHits([]);
        setMessage(
          caught instanceof ApiClientError
            ? caught.message
            : "Search is not available right now.",
        );
        setState("error");
      }
    },
    [organizationId],
  );

  function handleChange(value: string) {
    setQuery(value);
    setActiveIndex(0);

    window.clearTimeout(timer.current);

    const text = value.trim();

    if (text.length < MIN_SEARCH_LENGTH || !organizationId) {
      sequence.current += 1;
      setHits([]);
      setState("idle");

      return;
    }

    setState("loading");

    timer.current = window.setTimeout(() => {
      void runSearch(text);
    }, DEBOUNCE_MS);
  }

  const sections = useMemo(
    () => buildSections(query, hits),
    [query, hits],
  );

  const items = useMemo(
    () => sections.flatMap((section) => section.items),
    [sections],
  );

  const offsets = useMemo(
    () =>
      sections.map((_, index) =>
        sections
          .slice(0, index)
          .reduce((total, section) => total + section.items.length, 0),
      ),
    [sections],
  );

  const active = Math.min(activeIndex, Math.max(0, items.length - 1));

  function optionId(index: number) {
    return `${listId}-option-${index}`;
  }

  function go(href: string) {
    closePalette();
    router.push(href);
  }

  function moveTo(index: number) {
    setActiveIndex(index);

    document
      .getElementById(optionId(index))
      ?.scrollIntoView({ block: "nearest" });
  }

  function handleKeyDown(event: ReactKeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      closePalette();

      return;
    }

    if (items.length === 0) {
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      moveTo((active + 1) % items.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      moveTo((active - 1 + items.length) % items.length);
    } else if (event.key === "Home" && event.target !== inputRef.current) {
      event.preventDefault();
      moveTo(0);
    } else if (event.key === "Enter") {
      event.preventDefault();
      go(items[active].href);
    }
  }

  if (!open) {
    return null;
  }

  const trimmed = query.trim();
  const searching = trimmed.length >= MIN_SEARCH_LENGTH && !!organizationId;
  const noResults =
    searching && state === "ready" && hits.length === 0 && items.length === 0;

  return (
    <div
      className="uxPaletteBackdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          closePalette();
        }
      }}
    >
      <div
        className="uxPalette"
        role="dialog"
        aria-modal="true"
        aria-label="Search BuildFlow"
        onKeyDown={handleKeyDown}
      >
        <div className="uxPaletteInputRow">
          <input
            ref={inputRef}
            className="uxPaletteInput"
            type="text"
            role="combobox"
            aria-expanded="true"
            aria-controls={listId}
            aria-activedescendant={
              items.length > 0 ? optionId(active) : undefined
            }
            aria-autocomplete="list"
            autoComplete="off"
            spellCheck={false}
            placeholder="Search projects, tasks, clients, invoices…"
            value={query}
            onChange={(event) => handleChange(event.target.value)}
          />

          <kbd className="uxPaletteKey">esc</kbd>
        </div>

        <div
          className="uxPaletteBody"
          id={listId}
          role="listbox"
          aria-label="Search results"
        >
          {sections.map((section, sectionIndex) => (
            <div className="uxPaletteSection" key={section.id}>
              <div className="uxPaletteHeading" aria-hidden="true">
                {section.label}
              </div>

              {section.items.map((item, itemIndex) => {
                const index = offsets[sectionIndex] + itemIndex;
                const isActive = index === active;

                return (
                  <div
                    key={item.key}
                    id={optionId(index)}
                    role="option"
                    aria-selected={isActive}
                    className={
                      isActive ? "uxPaletteItem active" : "uxPaletteItem"
                    }
                    onMouseMove={() => {
                      if (!isActive) {
                        setActiveIndex(index);
                      }
                    }}
                    onClick={() => go(item.href)}
                  >
                    <span className="uxPaletteItemText">
                      <span className="uxPaletteItemTitle">{item.title}</span>

                      {item.subtitle && (
                        <span className="uxPaletteItemSub">
                          {item.subtitle}
                        </span>
                      )}
                    </span>

                    {item.status && (
                      <span className="uxPaletteBadge">
                        {statusLabel(item.status)}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          ))}

          {state === "loading" && searching && (
            <p className="uxPaletteNote" role="status">
              Searching…
            </p>
          )}

          {state === "error" && (
            <p className="uxPaletteNote error" role="alert">
              {message}
            </p>
          )}

          {noResults && (
            <p className="uxPaletteNote" role="status">
              Nothing found for “{trimmed}”.
            </p>
          )}

          {!organizationId && trimmed.length >= MIN_SEARCH_LENGTH && (
            <p className="uxPaletteNote">
              Join or select an organization to search its records.
            </p>
          )}

          {trimmed.length === 0 && (
            <p className="uxPaletteNote">
              Type at least {MIN_SEARCH_LENGTH} letters to search projects,
              tasks, clients, suppliers, invoices and purchase orders.
            </p>
          )}
        </div>

        <div className="uxPaletteFooter" aria-hidden="true">
          <span>
            <kbd className="uxPaletteKey">↑</kbd>
            <kbd className="uxPaletteKey">↓</kbd> navigate
          </span>

          <span>
            <kbd className="uxPaletteKey">↵</kbd> open
          </span>

          <span>
            <kbd className="uxPaletteKey">esc</kbd> close
          </span>
        </div>
      </div>
    </div>
  );
}
