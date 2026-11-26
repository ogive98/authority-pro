import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <main class="missing">
      <p class="wordmark">AUTHORITY</p>
      <h1>Page introuvable</h1>
      <p>L’adresse demandée n’existe pas ou n’est plus accessible.</p>
      <a routerLink="/">Revenir à l’accueil</a>
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
      color: var(--text-primary);
      font-size: 14px;
    }
  `,
})
export class NotFoundPage {}
