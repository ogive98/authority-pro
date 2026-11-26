import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiFailure } from '../../core/api/problem';
import { SessionService } from '../../core/auth/session.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginPage {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly alert = viewChild<ElementRef<HTMLElement>>('alert');

  readonly form = inject(FormBuilder).nonNullable.group({
    login: ['', [Validators.required, Validators.maxLength(200)]],
    password: ['', [Validators.required, Validators.maxLength(256)]],
  });
  readonly submitting = signal(false);
  readonly failure = signal<ApiFailure | null>(null);
  readonly showPassword = signal(false);
  readonly sessionState = this.session.state;

  constructor() {
    effect(() => {
      const alert = this.alert();
      if (alert && this.failure()) alert.nativeElement.focus({ preventScroll: true });
    });
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.failure.set(null);
    this.submitting.set(true);
    const { login, password } = this.form.getRawValue();
    void this.session.login(login, password).then((result) => {
      this.submitting.set(false);
      if (result.ok) {
        void this.router.navigateByUrl(this.returnUrl());
        return;
      }
      this.failure.set(result.failure);
    });
  }

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  private returnUrl(): string {
    const requested = this.route.snapshot.queryParamMap.get('returnUrl');
    return requested?.startsWith('/app') ? requested : '/app/tenants';
  }
}
