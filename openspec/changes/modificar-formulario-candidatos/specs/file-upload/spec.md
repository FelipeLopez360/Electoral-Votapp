# File Upload Specification

## Purpose

Provide standalone file and image upload capabilities to decouple binary uploads from complex JSON creation payloads, ensuring clean API contracts.

## Requirements

### Requirement: Standalone File Upload Endpoint

The system MUST provide an endpoint `POST /api/v1/files/upload` that accepts `multipart/form-data` containing a file and returns a hosted URL string.

#### Scenario: Successful image upload

- GIVEN an admin with a valid image file (e.g., candidate photo)
- WHEN they submit a `POST` request to `/api/v1/files/upload` with the file
- THEN the system MUST process and securely store the file
- AND return a JSON response containing the `url` (e.g., `{ "url": "..." }`) where the file is hosted

#### Scenario: Invalid file format or size

- GIVEN an admin with a file that exceeds size limits or has an unsupported format
- WHEN they submit the file to the upload endpoint
- THEN the system MUST reject the request with a validation error (HTTP 400)
