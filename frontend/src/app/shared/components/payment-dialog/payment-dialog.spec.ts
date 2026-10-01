import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { PaymentDialog } from './payment-dialog';

describe('PaymentDialog', () => {
  let fixture: ComponentFixture<PaymentDialog>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PaymentDialog>>;

  interface Internals {
    form: PaymentDialog['form'];
    confirm(): void;
  }

  beforeEach(() => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<PaymentDialog>>('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [PaymentDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { title: 'Encaisser', amountDue: 250.5 } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
    fixture = TestBed.createComponent(PaymentDialog);
    fixture.detectChanges();
  });

  function c(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  it('proposes the remaining balance and returns the payment', () => {
    expect(c().form.controls.amount.value).toBe(250.5);
    c().form.controls.reference.setValue('  CHQ-12 ');
    c().form.controls.method.setValue('CHECK');

    c().confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({ amount: 250.5, method: 'CHECK', reference: 'CHQ-12' });
  });

  it('refuses an amount above the balance or not positive', () => {
    c().form.controls.amount.setValue(250.501);
    c().confirm();
    c().form.controls.amount.setValue(0);
    c().confirm();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
