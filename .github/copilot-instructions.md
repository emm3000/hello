# Copilot Instructions

Wrapper minimo para GitHub Copilot en este repo.

## Estado vigente
- MVP en modo local-only.
- Sync remoto, pairing y bootstrap remoto removidos del trunk.
- Referencias a sync/Supabase runtime se consideran **legacy/archived, no activo**.

## Leer antes de sugerir cambios
- `AGENTS.md` para reglas operativas de agentes.
- `ARCHITECTURE.md` para arquitectura y boundaries.
- `LOCAL_FIRST.md` para contrato de runtime local-only.

## Guardrails rapidos
- Mantener boundaries: `app -> data`, `app -> domain`, `data -> domain`.
- Mantener `domain` JVM-only.
- No reintroducir supuestos de sync remoto activo en producto.
