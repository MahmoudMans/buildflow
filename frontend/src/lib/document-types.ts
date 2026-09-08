export type DocumentCategory =
  | "CONTRACT"
  | "DRAWING"
  | "PERMIT"
  | "QUOTE"
  | "INVOICE"
  | "RECEIPT"
  | "PHOTO"
  | "REPORT"
  | "INSURANCE"
  | "SAFETY"
  | "TECHNICAL"
  | "OTHER";

export type ProjectPhotoCategory =
  | "BEFORE"
  | "PROGRESS"
  | "AFTER"
  | "ISSUE"
  | "DELIVERY"
  | "OTHER";

export interface ProjectDocument {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  category: DocumentCategory;
  mimeType: string;
  sizeBytes: number;
  version: number;
  clientVisible: boolean;
  uploadedBy: string;
  createdAt: string;
}

export interface ProjectPhoto {
  id: string;
  projectId: string;
  fileId: string;
  caption: string | null;
  takenDate: string | null;
  category: ProjectPhotoCategory;
  clientVisible: boolean;
  uploadedBy: string;
  createdAt: string;
}
