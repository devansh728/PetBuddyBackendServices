# Pet Profile Backend - Dependencies and Configuration

## Required Dependencies

Add the following dependency to your `build.gradle` or `pom.xml`:

### Gradle
```gradle
dependencies {
    // Spring Integration for distributed locking
    implementation 'org.springframework.boot:spring-boot-starter-integration'
    implementation 'org.springframework.integration:spring-integration-redis'
}
```

### Maven
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-integration</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-redis</artifactId>
</dependency>
```

## Application Configuration

Add the following to your `application.yml`:

```yaml
aws:
  s3:
    region: us-east-1  # Your AWS region
    bucket: your-bucket-name
    presigned-url:
      upload-expiration: 900  # 15 minutes in seconds
      download-expiration: 3600  # 1 hour in seconds
    max-file-size: 10485760  # 10MB in bytes
    allowed-types: image/jpeg,image/png,image/jpg,application/pdf

scheduling:
  upload-cleanup:
    cron: "0 */30 * * * *"  # Every 30 minutes

spring:
  redis:
    host: localhost
    port: 6379
```

## Database Migration

Run the following SQL migration:

```sql
-- Add version columns for optimistic locking
ALTER TABLE pets ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add upload tracking columns to medical_documents
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS upload_status VARCHAR(20) DEFAULT 'CONFIRMED';
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS upload_session_id UUID;
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS upload_expires_at TIMESTAMP;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_medical_doc_upload_status ON medical_documents(upload_status);
CREATE INDEX IF NOT EXISTS idx_medical_doc_upload_expires ON medical_documents(upload_expires_at) WHERE upload_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_medical_doc_session_id ON medical_documents(upload_session_id);

-- Update existing records
UPDATE medical_documents SET upload_status = 'CONFIRMED' WHERE upload_status IS NULL;
```

## API Changes

### Old File Upload (DEPRECATED)
```
POST /api/v1/profile/me/pets/{petId}/documents
Content-Type: multipart/form-data
Body: file=<binary>
```

### New Presigned URL Workflow

#### 1. Request Upload URL
```
POST /api/v1/profile/me/pets/{petId}/documents/upload-url
Content-Type: application/json

{
  "fileName": "vaccination-record.pdf",
  "contentType": "application/pdf",
  "fileSizeBytes": 524288
}

Response:
{
  "uploadSessionId": "uuid",
  "presignedUrl": "https://s3.amazonaws.com/...",
  "storageKey": "medical-documents/uuid-vaccination-record.pdf",
  "expiresInSeconds": 900
}
```

#### 2. Upload to S3 (Client-side)
```
PUT <presignedUrl>
Content-Type: application/pdf
Body: <binary file data>
```

#### 3. Confirm Upload
```
POST /api/v1/profile/me/pets/{petId}/documents/confirm
Content-Type: application/json

{
  "uploadSessionId": "uuid"
}

Response:
{
  "id": 123,
  "fileName": "vaccination-record.pdf",
  "fileMimeType": "application/pdf",
  "fileSizeBytes": 524288,
  "downloadUrl": "https://s3.amazonaws.com/..."
}
```

## Benefits of New Architecture

1. **Scalability**: Backend doesn't handle file bytes, only metadata
2. **Performance**: Direct S3 upload, no backend bottleneck
3. **Cost**: Reduced bandwidth and compute costs
4. **Security**: Presigned URLs with expiration
5. **Multi-instance**: Redis cache handles consistency across pods
6. **Reliability**: Optimistic locking prevents race conditions
7. **Cleanup**: Automated removal of abandoned uploads
