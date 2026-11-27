import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink, TranslatePipe],
  template: `
    <main class="missing">
      <p class="wordmark">AUTHORITY</p>
      <h1>{{ 'notFound.title' | translate }}</h1>
      <p>{{ 'notFound.body' | translate }}</p>
      <a routerLink="/">{{ 'notFound.back' | translate }}</a>
    </main>
  `,
  styles: `
    .missing {
      min-height: 100dvh;
      display: grid;
      place-content: center;
      gap: 8px;
      padding: 24px;
    }

    .wordmark {
      margin: 0;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.32em;
      color: var(--text-tertiary);
    }

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    p {
      margin: 0;
      color: var(--text-secondary);
    }

    a {
      display: inline-flex;
      align-items: center;
      min-height: 24px;
      color: var(--text-primary);
      font-size: 14px;
    }
  `,
})
export class NotFoundPage {}
