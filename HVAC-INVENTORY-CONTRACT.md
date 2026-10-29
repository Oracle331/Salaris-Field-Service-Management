# HVAC Inventory and Job Preparation API Contract

## GET /api/inventory
Returns a list of stock items with their locations, filter sizes, quantities, and availability status.

### Response JSON
{
  "items": [
    {
      "id": "string",
      "name": "string",
      "stockLocation": "string",
      "filterSize": "string",
      "quantity": "number",
      "required": "number",
      "available": "number",
      "shortage": "number",
      "unknownSize": "boolean",
      "unknownQuantity": "boolean"
    }
  ]
}

## POST /api/inventory/transfers
Transfers stock items between office and work trucks. Requires a unique transfer ID for idempotency.

### Request JSON
{
  "transferId": "string",
  "items": [
    {
      "stockItemId": "string",
      "quantity": "number",
      "fromLocation": "string",
      "toLocation": "string"
    }
  ]
}

## GET /api/jobs/{id}/preparation
Returns job preparation details including installed equipment, filter sizes, quantities, tools, and a before-departure checklist.

### Response JSON
{
  "job": {
    "id": "string",
    "title": "string",
    "trade": "string",
    "status": "string",
    "clientName": "string",
    "address": "string",
    "date": "string",
    "timeWindow": "string",
    "summary": "string",
    "accessNotes": "string",
    "worker": "string",
    "clientAccount": "string",
    "equipment": [
      {
        "name": "string",
        "brand": "string",
        "model": "string",
        "serialNumber": "string",
        "location": "string",
        "filterSize": "string",
        "fittingSize": "string"
      }
    ],
    "filters": [
      {
        "size": "string",
        "quantity": "number",
        "required": "number",
        "available": "number",
        "shortage": "number",
        "unknownSize": "boolean",
        "unknownQuantity": "boolean"
      }
    ],
    "tools": [
      {
        "name": "string",
        "type": "string",
        "quantity": "number",
        "required": "number",
        "available": "number",
        "shortage": "number",
        "unknownQuantity": "boolean"
      }
    ],
    "checklist": [
      {
        "item": "string",
        "completed": "boolean"
      }
    ]
  }
}

## Compatibility
- Uses existing CSRF/session authentication and Spring Security.
- Maintains optimistic locking and access scoping.
- Ensures no changes to existing authentication and authorization mechanisms.