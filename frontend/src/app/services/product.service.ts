import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApiErrorResponse,
  ImportResponse,
  PageResponse,
  Product,
  ProductSummary
} from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly baseUrl = `${environment.apiUrl}/products`;

  constructor(private readonly http: HttpClient) {}

  getProducts(page: number, size: number, sort: string, search?: string): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PageResponse<Product>>(this.baseUrl, { params });
  }

  getSummary(): Observable<ProductSummary> {
    return this.http.get<ProductSummary>(`${this.baseUrl}/summary`);
  }

  importProducts(file: File): Observable<ImportResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportResponse>(`${this.baseUrl}/import`, formData);
  }
}

export function isApiError(error: unknown): error is { error: ApiErrorResponse } {
  return typeof error === 'object' && error !== null && 'error' in error;
}
