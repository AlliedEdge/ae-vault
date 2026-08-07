# Changelog - File Management Module

All notable changes to the file management module are documented here.

## [Unreleased]

### Added
- FileController REST API:
  - POST /api/v1/files/upload - Upload files (max 100MB)
  - GET /api/v1/files/{fileId}/download - Download files
  - GET /api/v1/files/{fileId} - Get file metadata
  - GET /api/v1/files - List files in folder with pagination
  - GET /api/v1/files/search - Search files by name
  - DELETE /api/v1/files/{fileId} - Delete files
- FolderController REST API:
  - POST /api/v1/folders - Create folder
  - GET /api/v1/folders/{folderId} - Get folder details
  - GET /api/v1/folders - List folders in parent
  - PATCH /api/v1/folders/{folderId}/rename - Rename folder
  - PATCH /api/v1/folders/{folderId}/move - Move folder to new parent
  - DELETE /api/v1/folders/{folderId} - Delete folder and contents
- FileService with business logic:
  - File upload with duplicate detection (SHA-256 hash)
  - File download with streaming
  - File deletion with storage cleanup
  - File listing with pagination
  - File search functionality
  - Storage usage tracking
- FolderService with business logic:
  - Folder creation with path management
  - Folder retrieval
  - Folder listing
  - Folder rename
  - Folder move with circular reference prevention
  - Folder deletion (cascade)
- StorageService abstraction layer
- LocalStorageService implementation:
  - File system storage
  - Directory management
  - File stream handling
  - Storage cleanup
- File and folder DTOs:
  - FileUploadResponse
  - FileMetadataResponse
  - FolderRequest
  - FolderResponse
- Folder entity with hierarchical structure:
  - Parent-child relationships
  - Folder path tracking
  - Timestamps
- FolderRepository with custom queries
- Database migrations:
  - V8__Create_folders_table.sql
  - V9__Create_file_metadata_table.sql

### Changed
- Enhanced FileMetadata entity with additional fields
- Updated FileMetadataRepository with improved query methods

---

## [0.2.0] - 2026-08-05

### Added
- Initial file module structure
- FileMetadata entity with:
  - File identification (UUID)
  - File name and MIME type
  - File size and SHA-256 hash
  - Storage key and duplicate flag
  - Download count tracking
  - Timestamps
- FileStatus enum (ACTIVE, DELETED, ARCHIVED)
- FileMetadataRepository with basic queries
- FileStorageService interface
- LocalFileStorageService basic implementation
- Database migration V3__Create_file_metadata_table.sql
