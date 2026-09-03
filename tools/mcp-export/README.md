# Out-of-band collectors

Some of the most useful Korean providers cannot be reached by the deployed backend. They are
queried out of band, and the results cross into the system through the durable import
boundary described in [ADR 0003](../../docs/adr/0003-import-boundary.md) rather than through
a runtime dependency on whatever tool did the collecting.

## The contract

A collector writes one JSON file:

```json
{
  "provider": "pathsdog",
  "fetchedAt": "2026-09-03T00:00:00Z",
  "collector": "mcp-pathsdog-2026-09-03",
  "records": [ { "...": "provider-shaped record" } ]
}
```

`build-ndjson.mjs` maps that into the `RawJobRecord` import schema and writes NDJSON:

```bash
node tools/mcp-export/build-ndjson.mjs collected/pathsdog.json imports/pathsdog.ndjson
```

The whole provider record is preserved verbatim in `rawPayload`, so nothing the mapper does
not understand is lost. A record with no title or no company is dropped at this stage rather
than sent, because the backend would reject it as a normalization failure anyway.

## Importing

```bash
curl -X POST "http://localhost:8080/api/internal/ingestion/import?source=pathsdog&collector=mcp-pathsdog-2026-09-03" \
  -H "X-Internal-Token: $INTERNAL_API_TOKEN" \
  -H "Content-Type: application/x-ndjson" \
  --data-binary @imports/pathsdog.ndjson
```

The response is the ingestion outcome: records received, records normalized, new jobs,
updated jobs, duplicates merged, failures. Malformed lines are stored in
`ingestion_failures` with the offending line, so an import never silently loses a record.

## Supported providers

| `provider` | Maps into source | Notes |
| --- | --- | --- |
| `pathsdog` | `pathsdog` | Carries the employer's original URL, which is what lets rung 1 of the deduplication ladder link it to the employer's own board |
| `jobkorea` | `jobkorea` | External id is recovered from the `GI_Read/<id>` path when not supplied |
| `saramin` | `saramin` | External id is recovered from `rec_idx` when not supplied |
| `linkedin` | `linkedin` | Company LinkedIn URL is kept as a company identifier |
| `indeed` | `indeed` | No external id is emitted: the provider's ids are per-response, and the source registry marks it `stable_external_id = false` |
| `freehire` | `freehire` | The underlying ATS board token is extracted into a company identifier, which is how an aggregator row links to the employer's own board |

## Adding a provider

Add a mapper to `MAPPERS` in `build-ndjson.mjs` and a row to the `sources` table. The mapper's
only job is to move provider fields into the `raw*` fields; it must not normalize, infer or
clean anything. Normalization belongs to the backend, where its output is versioned and its
evidence is recorded.
