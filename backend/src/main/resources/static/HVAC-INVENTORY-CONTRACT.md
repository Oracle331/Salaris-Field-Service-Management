# HVAC Inventory API Contracts

## GET /api/inventory
Returns a list of stock items with their locations, filter sizes, quantities, and availability status.

### Response JSON Structure
```json
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
      "unknownData": "boolean"
    }
  ]
}
```

## POST /api/inventory/transfers
Transfers stock items between locations. Requires stock item ID, source and destination locations, and quantity.

### Request JSON Structure
```json
{
  "stockItemId": "string",
  "fromLocation": "string",
  "toLocation": "string",
  "quantity": "number"
}
```

## GET /api/jobs/{id}/preparation
Returns job preparation details including required equipment, filter sizes, quantities, and tool checklist.

### Response JSON Structure
```json
{
  "jobId": "string",
  "equipment": [
    {
      "id": "string",
      "name": "string",
      "location": "string",
      "filterSize": "string",
      "quantity": "number",
      "required": "number",
      "available": "number",
      "shortage": "number",
      "unknownData": "boolean"
    }
  ],
  "tools": [
    {
      "id": "string",
      "name": "string",
      "location": "string",
      "quantity": "number",
      "required": "number",
      "available": "number",
      "shortage": "number",
      "unknownData": "boolean"
    }
  ],
  "checklist": [
    {
      "id": "string",
      "item": "string",
      "completed": "boolean"
    }
  ]
}
```

## Security and Compatibility
- CSRF protection is enforced using Spring Security.
- Session-based authentication is required for all endpoints.
- Role-based access control is in place to restrict inventory and job preparation operations to authorized users.