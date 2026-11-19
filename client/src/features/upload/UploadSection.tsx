import { useCallback, useEffect, useRef, useState } from "react";
import { Icon } from "../../components/Icon";
import {
  documentTypes,
  maxFileCount,
  maxFileSize,
  uploadAccept,
  uploadExtensions,
} from "../../constants/formOptions";
import {
  analyzeDocuments,
  getApiErrorMessage,
  uploadDocuments,
  type ApplicantExtractionResponse,
} from "../../services/api";
import type { DocumentType, UploadedDocument } from "../../types/applicant";

type Props = {
  onEmployerCertificateChange: (present: boolean) => void;
  onAnalysisResult: (result: ApplicantExtractionResponse) => void;
};

function formatSize(bytes: number) {
  return bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))} Ko`
    : `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

function createDocument(file: File): UploadedDocument {
  return {
    id: `${file.name}-${file.lastModified}-${Math.random().toString(36).slice(2)}`,
    name: file.name,
    size: file.size,
    type: file.type,
    file,
    progress: 5,
    status: "uploading",
    documentType: "",
  };
}

export function UploadSection({
  onEmployerCertificateChange,
  onAnalysisResult,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const previousCertificateRef = useRef(false);
  const activeAnalysisIds = useRef(new Set<string>());
  const [documents, setDocuments] = useState<UploadedDocument[]>([]);
  const [dragging, setDragging] = useState(false);
  const [notice, setNotice] = useState("");

  const updateDocuments = useCallback(
    (
      ids: string[],
      update: (document: UploadedDocument) => UploadedDocument,
    ) => {
      setDocuments((current) =>
        current.map((document) =>
          ids.includes(document.id) ? update(document) : document,
        ),
      );
    },
    [],
  );

  async function analyzeUploaded(uploadId: string, ids: string[]) {
    if (activeAnalysisIds.current.has(uploadId)) return;
    activeAnalysisIds.current.add(uploadId);
    updateDocuments(ids, (document) => ({
      ...document,
      status: "uploading",
      progress: 75,
      errorMessage: undefined,
    }));
    setNotice("");

    try {
      const result = await analyzeDocuments(uploadId);
      updateDocuments(ids, (document) => ({
        ...document,
        status: "success",
        progress: 100,
        uploadId,
        errorMessage: undefined,
      }));
      onAnalysisResult(result);
    } catch (error) {
      const message = getApiErrorMessage(error);
      updateDocuments(ids, (document) => ({
        ...document,
        status: "error",
        uploadId,
        errorMessage: message,
      }));
      setNotice(`L’analyse des documents a échoué : ${message}`);
    } finally {
      activeAnalysisIds.current.delete(uploadId);
    }
  }

  async function uploadBatch(entries: UploadedDocument[]) {
    const ids = entries.map((entry) => entry.id);
    try {
      const response = await uploadDocuments(
        entries.map((entry) => entry.file),
        (progress) =>
          updateDocuments(ids, (document) => ({ ...document, progress })),
      );
      updateDocuments(ids, (document) => ({
        ...document,
        progress: 65,
        uploadId: response.uploadId,
        errorMessage: undefined,
      }));
      await analyzeUploaded(response.uploadId, ids);
    } catch (error) {
      const message = getApiErrorMessage(error);
      updateDocuments(ids, (document) => ({
        ...document,
        status: "error",
        errorMessage: message,
      }));
      setNotice(`Le dépôt des documents a échoué : ${message}`);
    }
  }

  function addFiles(fileList: FileList | File[]) {
    const incoming = Array.from(fileList);
    const valid: UploadedDocument[] = [];
    const rejected: string[] = [];
    const availableSlots = Math.max(0, maxFileCount - documents.length);

    incoming.forEach((file) => {
      const extension = file.name.split(".").pop()?.toLowerCase();
      if (!extension || !uploadExtensions.includes(extension)) {
        rejected.push(`${file.name} : format non accepté (PDF, JPG ou PNG).`);
      } else if (file.size > maxFileSize) {
        rejected.push(`${file.name} : la taille maximale est de 20 Mo.`);
      } else if (documents.length + valid.length >= maxFileCount) {
        rejected.push(`Le dépôt est limité à ${maxFileCount} fichiers.`);
      } else if (valid.length >= availableSlots) {
        rejected.push(`Le dépôt est limité à ${maxFileCount} fichiers.`);
      } else {
        valid.push(createDocument(file));
      }
    });

    setNotice(rejected.join(" "));
    if (!valid.length) return;

    setDocuments((current) => [...current, ...valid]);
    void uploadBatch(valid);
  }

  async function retryDocument(document: UploadedDocument) {
    if (document.uploadId) {
      const ids = documents
        .filter((item) => item.uploadId === document.uploadId)
        .map((item) => item.id);
      await analyzeUploaded(document.uploadId, ids);
      return;
    }

    updateDocuments([document.id], (item) => ({
      ...item,
      status: "uploading",
      progress: 5,
      errorMessage: undefined,
    }));
    await uploadBatch([{ ...document, status: "uploading" }]);
  }

  const hasEmployerCertificate = documents.some(
    (document) =>
      (document.documentType === "Attestation employeur" ||
        document.documentType === "Attestation de travail") &&
      document.status !== "error",
  );

  useEffect(() => {
    if (previousCertificateRef.current === hasEmployerCertificate) return;
    previousCertificateRef.current = hasEmployerCertificate;
    onEmployerCertificateChange(hasEmployerCertificate);
  }, [hasEmployerCertificate, onEmployerCertificateChange]);

  return (
    <section
      className="card upload-card"
      id="documents"
      aria-labelledby="upload-title"
    >
      <div className="section-heading">
        <div>
          <div className="eyebrow">ÉTAPE 1 · DOCUMENTS</div>
          <h2 id="upload-title">Vos documents</h2>
          <p>
            Ajoutez vos justificatifs pour les transmettre au service d’analyse.
          </p>
        </div>
        <span className="optional-badge">Facultatif</span>
      </div>
      <div
        className={`drop-zone${dragging ? " is-dragging" : ""}`}
        onDragEnter={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragOver={(event) => event.preventDefault()}
        onDragLeave={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget as Node | null))
            setDragging(false);
        }}
        onDrop={(event) => {
          event.preventDefault();
          setDragging(false);
          addFiles(event.dataTransfer.files);
        }}
      >
        <div className="upload-icon">
          <Icon name="upload" />
        </div>
        <div className="drop-copy">
          <strong>Glissez-déposez vos fichiers ici</strong>
          <span>ou</span>
          <button
            type="button"
            className="text-button"
            onClick={() => inputRef.current?.click()}
          >
            Parcourir vos fichiers
          </button>
        </div>
        <p>PDF, JPG ou PNG · 20 Mo maximum par fichier · 20 fichiers maximum</p>
        <p>
          Les fichiers sont envoyés au backend local puis transmis au service
          d’analyse. Aucun OCR n’est exécuté dans le navigateur.
        </p>
        <p>
          Retirer un fichier de cette liste ne supprime pas la copie déjà
          transmise au stockage du backend.
        </p>
        <input
          ref={inputRef}
          type="file"
          accept={uploadAccept}
          multiple
          className="visually-hidden"
          aria-label="Choisir des justificatifs à ajouter"
          onChange={(event) => {
            if (event.target.files) addFiles(event.target.files);
            event.target.value = "";
          }}
        />
      </div>
      {notice && (
        <p className="upload-notice" role="alert">
          {notice}
        </p>
      )}
      {documents.length > 0 && (
        <div className="file-list" aria-live="polite">
          <h3>Fichiers ajoutés ({documents.length})</h3>
          {documents.map((document) => (
            <div className={`file-item ${document.status}`} key={document.id}>
              <span className="file-icon">
                <Icon name="file" />
              </span>
              <span className="file-meta">
                <strong>{document.name}</strong>
                <span>
                  {formatSize(document.size)}
                  {document.status === "uploading" &&
                    " · Envoi ou analyse en cours…"}
                  {document.status === "success" && " · Analysé"}
                  {document.status === "error" && " · Échec"}
                </span>
                {document.errorMessage && (
                  <span className="file-error">{document.errorMessage}</span>
                )}
                {document.status === "uploading" && (
                  <span className="progress-track">
                    <span
                      className="progress-fill"
                      style={{ width: `${document.progress}%` }}
                    />
                  </span>
                )}
              </span>
              <select
                className="file-type-select"
                aria-label={`Type de document pour ${document.name}`}
                value={document.documentType}
                onChange={(event) => {
                  const documentType = event.target.value as DocumentType | "";
                  setDocuments((current) =>
                    current.map((item) =>
                      item.id === document.id
                        ? { ...item, documentType }
                        : item,
                    ),
                  );
                }}
              >
                <option value="">Type de document…</option>
                {documentTypes.map((type) => (
                  <option value={type} key={type}>
                    {type}
                  </option>
                ))}
              </select>
              <span
                className={`file-status ${document.status}`}
                aria-label={
                  document.status === "success"
                    ? "Analysé"
                    : document.status === "error"
                      ? "Échec"
                      : "En cours"
                }
              >
                {document.status === "success" && <Icon name="check" />}
                {document.status === "error" && "À réessayer"}
                {document.status === "uploading" && "En cours"}
              </span>
              {document.status === "error" ? (
                <button
                  type="button"
                  className="file-retry"
                  onClick={() => void retryDocument(document)}
                >
                  Réessayer
                </button>
              ) : (
                <button
                  type="button"
                  className="icon-button remove-file"
                  onClick={() =>
                    setDocuments((current) =>
                      current.filter((item) => item.id !== document.id),
                    )
                  }
                  aria-label={`Retirer ${document.name} de la liste`}
                >
                  <Icon name="trash" />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
      <div className="document-types">
        <p>Documents utiles pour votre inscription</p>
        <ul>
          {documentTypes.map((type) => (
            <li key={type}>{type}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}
