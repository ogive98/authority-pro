import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SessionService } from '../../core/auth/session.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly session = inject(SessionService);
  private readonly scope = inject(ScopeService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  readonly profile = this.session.profile;
  readonly activeScope = this.scope.scope;
  readonly loggingOut = signal(false);

  scopeLabel(): string {
    const scope = this.activeScope();
    if (!scope) return this.i18n.instant('common.noActiveScope');
    return scope.companyName ? `${scope.tenantName} — ${scope.companyName}` : scope.tenantName;
  }

  logout(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    void this.session
      .logout()
      .then(() => {
        this.scope.clear();
        return this.router.navigateByUrl('/login');
      })
      .finally(() => this.loggingOut.set(false));
  }
}
