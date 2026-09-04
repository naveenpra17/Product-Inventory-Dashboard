import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { ValidationError } from '../../models/product.models';

export interface ImportDialogData {
  file: File;
}

export interface ImportDialogResult {
  success: boolean;
  importedCount?: number;
}

@Component({
  selector: 'app-import-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatListModule
  ],
  templateUrl: './import-dialog.component.html',
  styleUrl: './import-dialog.component.scss'
})
export class ImportDialogComponent {
  loading = false;
  successMessage = '';
  errorMessage = '';
  validationErrors: ValidationError[] = [];
  importedCount = 0;

  constructor(
    private readonly dialogRef: MatDialogRef<ImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ImportDialogData
  ) {}

  get fileType(): string {
    const name = this.data.file.name.toLowerCase();
    if (name.endsWith('.xlsx')) {
      return 'Excel (.xlsx)';
    }
    if (name.endsWith('.csv')) {
      return 'CSV (.csv)';
    }
    return 'Unknown';
  }

  setLoading(loading: boolean): void {
    this.loading = loading;
  }

  setSuccess(message: string, importedCount: number): void {
    this.loading = false;
    this.successMessage = message;
    this.importedCount = importedCount;
    this.errorMessage = '';
    this.validationErrors = [];
    this.dialogRef.disableClose = false;
  }

  setError(message: string, errors: ValidationError[] = []): void {
    this.loading = false;
    this.errorMessage = message;
    this.validationErrors = errors;
    this.successMessage = '';
    this.dialogRef.disableClose = false;
  }

  close(): void {
    this.dialogRef.close({ success: !!this.successMessage, importedCount: undefined } satisfies ImportDialogResult);
  }

  closeWithSuccess(importedCount: number): void {
    this.dialogRef.close({ success: true, importedCount } satisfies ImportDialogResult);
  }
}
