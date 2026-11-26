import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-overview-page',
  imports: [RouterLink],
  templateUrl: './overview.html',
  styleUrl: './overview.css',
})
export class OverviewPage {
  private readonly scope = inject(ScopeService);
  readonly access = this.scope.scope;
}
