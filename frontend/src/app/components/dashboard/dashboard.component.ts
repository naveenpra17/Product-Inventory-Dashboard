import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { Product, ProductSummary } from '../../models/product.models';
import { isApiError, ProductService } from '../../services/product.service';
import { ImportDialogComponent } from '../import-dialog/import-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  displayedColumns = [
    'productSku',
    'productName',
    'category',
    'purchaseDate',
    'unitPrice',
    'quantity',
    'stockAgeDays'
  ];

  dataSource = new MatTableDataSource<Product>([]);
  summary: ProductSummary | null = null;

  loadingProducts = false;
  loadingSummary = false;
  productsError = '';
  summaryError = '';

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  sortActive = 'purchaseDate';
  sortDirection: 'asc' | 'desc' = 'desc';
  searchTerm = '';

  constructor(
    private readonly productService: ProductService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadSummary();
    this.loadProducts();
  }

  loadSummary(): void {
    this.loadingSummary = true;
    this.summaryError = '';
    this.productService.getSummary()
      .pipe(finalize(() => this.loadingSummary = false))
      .subscribe({
        next: (summary: ProductSummary) => this.summary = summary,
        error: () => {
          this.summaryError = 'Unable to load summary data.';
          this.summary = null;
        }
      });
  }

  loadProducts(): void {
    this.loadingProducts = true;
    this.productsError = '';
    const sort = `${this.sortActive},${this.sortDirection}`;

    this.productService.getProducts(this.pageIndex, this.pageSize, sort, this.searchTerm)
      .pipe(finalize(() => this.loadingProducts = false))
      .subscribe({
        next: (page: { content: Product[]; totalElements: number }) => {
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
        },
        error: () => {
          this.productsError = 'Unable to load products. Please try again.';
          this.dataSource.data = [];
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  onSortChange(sort: Sort): void {
    if (!sort.active || !sort.direction) {
      return;
    }
    this.sortActive = sort.active;
    this.sortDirection = sort.direction;
    this.pageIndex = 0;
    this.loadProducts();
  }

  onSearch(): void {
    this.pageIndex = 0;
    this.loadProducts();
  }

  triggerImport(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';

    if (!file) {
      return;
    }

    const lower = file.name.toLowerCase();
    if (!lower.endsWith('.xlsx') && !lower.endsWith('.csv')) {
      this.snackBar.open('Only .xlsx and .csv files are supported.', 'Dismiss', { duration: 5000 });
      return;
    }

    const dialogRef = this.dialog.open(ImportDialogComponent, {
      width: '560px',
      disableClose: true,
      data: { file }
    });

    const dialogInstance = dialogRef.componentInstance!;
    dialogInstance.setLoading(true);

    this.productService.importProducts(file).subscribe({
      next: (response: { message: string; importedCount: number }) => {
        dialogInstance.setSuccess(response.message, response.importedCount);
        this.snackBar.open(response.message, 'Dismiss', { duration: 4000 });
        this.loadSummary();
        this.loadProducts();
      },
      error: (error) => {
        if (isApiError(error) && error.error) {
          dialogInstance.setError(error.error.message || 'Import failed.', error.error.errors || []);
        } else {
          dialogInstance.setError('Import failed. Please try again.');
        }
      }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        this.loadSummary();
        this.loadProducts();
      }
    });
  }
}
