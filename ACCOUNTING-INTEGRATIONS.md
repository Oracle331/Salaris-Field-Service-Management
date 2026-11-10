# Invoicing and accounting integration proposal

This proposal covers adding invoicing and accounting. The app currently has no accounting connection.

## Recommended starting point

Start with QuickBooks Online for contractors whose bookkeeper already uses it. Add Xero through the same adapter interface when a customer needs it. Consider Zoho Books for customers who already use its ecosystem. Avoid requiring a contractor to change accounting systems just to adopt Salaris.

Salaris should own operational information: clients, service locations, equipment, jobs, labor, parts, and the draft invoice. The connected accounting system should own posted invoices, official invoice numbers, tax accounting, payment allocation, credit notes, and the general ledger. Keep accounting credentials and provider records separate for each contractor company when multi-organization support is added.

## Provider comparison

| Tool | Fit for this product | Integration path | Cost considerations |
| --- | --- | --- | --- |
| QuickBooks Online | Recommended first adapter when customers already use QuickBooks | Accounting REST API for customers, items, invoices, and payment records; OAuth connection; webhook-driven reconciliation | Intuit's Builder tier has a capped CorePlus allowance. API classifications distinguish unmetered Core writes from metered CorePlus reads. Contractor subscriptions and payment processing are separate costs. |
| Xero | Second adapter for Xero customers | Contacts and accounting invoices API; native draft invoice state; OAuth; invoice/contact webhooks | Developer Starter permits five connections at no charge; paid plans scale by connection count and egress. Business subscriptions are separate. |
| Zoho Books | Alternative for contractors already using Zoho | Contacts/invoices REST API with OAuth and organization-specific requests | Check the contractor's Books plan, API limits, regional endpoints, and gateway costs before committing. |

These are integration-fit recommendations, not claims that one provider has the lowest total cost for every contractor. Do not assume a developer allowance covers the contractor's accounting subscription. Verify partner pricing and production review requirements before commercial launch.

## Proposed user flow

1. Worker completes a job and records work performed, labor, quantities, and parts. Equipment/location photos remain operational records; only selected customer-facing attachments should be considered for an invoice.
2. Office selects **Prepare invoice**. Salaris creates a local draft with a client, billing address, service location, job reference, line items, discounts, currency, tax-code mappings, and payment terms. Completion never automatically sends an invoice.
3. Office checks the draft and approves an export. The adapter matches or creates the accounting customer and service items, then creates the provider invoice once. A QuickBooks invoice should be treated as an accounting transaction on export; do not assume it has Xero's draft lifecycle.
4. Accounting software sends the invoice and provides its payment workflow. Salaris displays the external invoice reference, balance, and status. Keep payment card handling out of Salaris initially.
5. Signed webhooks notify Salaris of changes; a background reconciliation job confirms balances and catches missed events. Provide a visible sync status, last successful sync, and retry action.

## Spring Boot implementation plan

- Add `InvoiceDraft` and `InvoiceLine` with immutable approved snapshots and decimal money amounts (`BigDecimal`), not floating point. Separate service completion, billing approval, accounting export, and settlement states.
- Map stable local `clientId`, `locationId`, `jobId`, and invoice IDs to each provider's customer, item, invoice, and company/tenant IDs. Do not use an editable email address as the synchronization key.
- Implement an `AccountingProvider` interface for `connect`, `disconnect`, `upsertCustomer`, `createInvoice`, `getInvoice`, and `reconcilePayments`. Build QuickBooks first without coupling job controllers to its API types.
- Keep encrypted OAuth credentials on the server. Request only needed scopes, validate OAuth state, coordinate token refresh, handle disconnects, and keep secrets out of logs and the browser.
- Use a transactional outbox with a unique local operation ID, provider idempotency support where available, persisted external IDs, and cautious reconciliation after timeouts. A retry must not create a second invoice.
- Validate webhook signatures and persist event IDs before processing. Deduplicate callbacks, retry transient failures with backoff, honor provider rate limits, and record who approved every export. Never let a payment webhook alter operational job completion.
- Keep provider-owned financial changes authoritative. Display conflicts for review instead of overwriting payments or edits made by a bookkeeper. Handle partial payments, credits, voids, and refunds explicitly.
- For production OAuth callbacks and webhooks, use a stable HTTPS hostname and durable server process. Temporary Cloudflare demo links are useful for manual sandbox trials but change when recreated and are unsuitable as a permanent callback address.

## Low-cost delivery sequence

First build the provider-neutral invoice draft and approval screen, then a QuickBooks sandbox adapter, then payment/status reconciliation. Add Xero only when needed by a target customer. A reviewed CSV export can be an interim option where a provider's import format supports the required fields, with duplicate controls and clear mapping instructions. Avoid constant full-account polling; reconcile changed records and use webhooks where supported.

Before implementation, establish which accounting products the first contractors already use, whether they invoice per visit or per project, how they handle deposits and partial billing, and which tax, item, and account mappings their bookkeeper expects.

## Primary sources

- [QuickBooks invoice API](https://developer.intuit.com/docs/api/accounting/invoice)
- [QuickBooks webhooks configuration](https://static.developer.intuit.com/output_html/qbo/docs/develop/webhooks/configure-webhooks.html)
- [QuickBooks API limits](https://static.developer.intuit.com/output_html/qbo/docs/learn/limits-and-throttles.html)
- [Intuit partner program: API classifications and fees](https://developer.intuit.com/cmsimages/partnerprogram/downloads/Intuit-Developer-Partner-Program_Benefits-Guide.pdf)
- [Xero invoice API](https://developer.xero.com/documentation/api/accounting/invoices)
- [Xero webhooks](https://developer.xero.com/documentation/guides/webhooks/overview)
- [Xero developer pricing](https://developer.xero.com/pricing)
- [Zoho Books invoices](https://www.zoho.com/books/api/v3/invoices/)
- [Zoho Books OAuth](https://www.zoho.com/books/api/v3/oauth/)
