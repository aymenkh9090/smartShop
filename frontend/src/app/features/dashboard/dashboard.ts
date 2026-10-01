import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section>
      <h2>Tableau de bord</h2>
      <p>Les indicateurs seront ajoutes en phase 8.</p>
    </section>
  `,
})
export class Dashboard {}
