"""PDF text extraction.

Pulls plain text out of uploaded PDFs so the agents (and the Verifier's
grounding check) have a searchable source of truth. We keep page boundaries as
markers so evidence can later be traced to a page.
"""
from __future__ import annotations

import base64
import io

from pypdf import PdfReader

from app.domain.models import InputDocument


class ParsedDocument:
    def __init__(self, filename: str, text: str, page_count: int):
        self.filename = filename
        self.text = text
        self.page_count = page_count


def parse_document(doc: InputDocument) -> ParsedDocument:
    """Decode a base64 PDF and extract its text."""
    raw = base64.b64decode(doc.content_base64)
    reader = PdfReader(io.BytesIO(raw))
    pages: list[str] = []
    for i, page in enumerate(reader.pages, start=1):
        text = page.extract_text() or ""
        pages.append(f"[Page {i}]\n{text}")
    full_text = "\n\n".join(pages)
    return ParsedDocument(
        filename=doc.filename,
        text=full_text,
        page_count=len(reader.pages),
    )


def parse_documents(docs: list[InputDocument]) -> list[ParsedDocument]:
    return [parse_document(d) for d in docs]


def combine_text(parsed: list[ParsedDocument]) -> str:
    """Concatenate all document text into one corpus the agents read from."""
    blocks = []
    for p in parsed:
        blocks.append(f"===== DOCUMENT: {p.filename} =====\n{p.text}")
    return "\n\n".join(blocks)
