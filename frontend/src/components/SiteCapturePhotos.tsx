import { useId } from "react";
import type { ChangeEvent } from "react";

import {
  CAPTION_MAX,
  PHOTO_CATEGORIES,
  formatSize,
} from "@/lib/site-capture";
import type { CapturePhoto } from "@/lib/site-capture";
import type { SitePhotoCategory } from "@/lib/site-operation-types";

type PhotoPatch = Partial<Pick<CapturePhoto, "category" | "caption" | "clientVisible">>;

const STATUS_LABEL: Record<CapturePhoto["status"], string> = {
  ready: "Ready",
  uploading: "Sending...",
  done: "Sent",
  failed: "Not sent",
};

const STATUS_TONE: Record<CapturePhoto["status"], string> = {
  ready: "neutral",
  uploading: "info",
  done: "success",
  failed: "danger",
};

function PhotoCard({
  photo,
  position,
  locked,
  onRemove,
  onChange,
}: {
  photo: CapturePhoto;
  position: number;
  locked: boolean;
  onRemove: (id: string) => void;
  onChange: (id: string, patch: PhotoPatch) => void;
}) {
  const id = useId();
  // Once a photo is on its way or sent, its details can no longer change.
  const frozen = locked || photo.status === "uploading" || photo.status === "done";

  return (
    <li className="uxCapturePhoto">
      <div
        className="uxCapturePhotoImage"
        role="img"
        aria-label={`Photo ${position}`}
        style={{ backgroundImage: `url("${photo.dataUrl}")` }}
      />

      <div className="uxCapturePhotoBody">
        <div className="uxCapturePhotoMeta">
          <strong>{photo.fileName}</strong>

          <span>{formatSize(photo.sizeBytes)}</span>

          <span className={`uxStatusBadge uxCapturePhotoStatus ${STATUS_TONE[photo.status]}`}>
            <span className="uxStatusDot" aria-hidden="true" />
            {STATUS_LABEL[photo.status]}
          </span>
        </div>

        <div className="uxCaptureField">
          <label htmlFor={`${id}-category`}>Category</label>

          <select
            id={`${id}-category`}
            value={photo.category}
            disabled={frozen}
            onChange={(event: ChangeEvent<HTMLSelectElement>) =>
              onChange(photo.id, { category: event.target.value as SitePhotoCategory })
            }
          >
            {PHOTO_CATEGORIES.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </div>

        <div className="uxCaptureField">
          <label htmlFor={`${id}-caption`}>Caption</label>

          <input
            id={`${id}-caption`}
            type="text"
            value={photo.caption}
            maxLength={CAPTION_MAX}
            disabled={frozen}
            autoComplete="off"
            placeholder="What does it show?"
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              onChange(photo.id, { caption: event.target.value })
            }
          />
        </div>

        <label className="uxCaptureCheck" htmlFor={`${id}-client`}>
          <input
            id={`${id}-client`}
            type="checkbox"
            checked={photo.clientVisible}
            disabled={frozen}
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              onChange(photo.id, { clientVisible: event.target.checked })
            }
          />
          Show this photo to the client
        </label>

        {photo.error ? (
          <p className="uxFieldError" role="alert">
            {photo.error}
          </p>
        ) : null}

        {!frozen ? (
          <button
            type="button"
            className="uxLinkButton uxCaptureRemove"
            aria-label={`Remove photo ${position}`}
            onClick={() => onRemove(photo.id)}
          >
            Remove
          </button>
        ) : null}
      </div>
    </li>
  );
}

// Step 3: take or choose photos and say what each one shows.
export function SiteCapturePhotos({
  photos,
  preparing,
  notice,
  maxPhotos,
  locked,
  onAddFiles,
  onRemove,
  onChange,
}: {
  photos: CapturePhoto[];
  // How many pictures are being shrunk right now.
  preparing: number;
  // A message about pictures that could not be added.
  notice: string;
  maxPhotos: number;
  locked: boolean;
  onAddFiles: (files: File[]) => void;
  onRemove: (id: string) => void;
  onChange: (id: string, patch: PhotoPatch) => void;
}) {
  const full = photos.length + preparing >= maxPhotos;
  const disabled = locked || full;

  function pick(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);

    // Allow the same picture to be chosen again later.
    event.target.value = "";

    if (files.length > 0) {
      onAddFiles(files);
    }
  }

  return (
    <div className="uxCapturePhotos">
      <div className="uxCaptureAddRow">
        <label className="uxCaptureAdd primary">
          <input
            className="uxSrOnly"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            capture="environment"
            disabled={disabled}
            onChange={pick}
          />
          <span>Take a photo</span>
        </label>

        <label className="uxCaptureAdd">
          <input
            className="uxSrOnly"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            disabled={disabled}
            onChange={pick}
          />
          <span>Choose from gallery</span>
        </label>
      </div>

      <p className="uxCaptureHint">
        Photos are made smaller on your phone before they are sent, so they upload quickly.
        {" "}
        {photos.length + preparing} of {maxPhotos} photos.
      </p>

      {preparing > 0 ? (
        <p className="uxCaptureHint" role="status">
          Getting {preparing === 1 ? "a photo" : `${preparing} photos`} ready...
        </p>
      ) : null}

      {notice ? (
        <p className="uxFieldError" role="alert">
          {notice}
        </p>
      ) : null}

      {photos.length === 0 && preparing === 0 ? (
        <p className="uxCaptureEmpty">
          No photos yet. You can send the report without them.
        </p>
      ) : null}

      {photos.length > 0 ? (
        <ul className="uxCapturePhotoList" aria-label="Photos in this report">
          {photos.map((photo, index) => (
            <PhotoCard
              key={photo.id}
              photo={photo}
              position={index + 1}
              locked={locked}
              onRemove={onRemove}
              onChange={onChange}
            />
          ))}
        </ul>
      ) : null}
    </div>
  );
}
