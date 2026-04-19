import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export interface PaymentRecord {
    id: string;
    planName: string;
    amount: number;
    currency: string;
    dateTime: string;
    status: 'Successful' | 'Failed' | 'Pending';
}

interface PaymentState {
    records: PaymentRecord[];
    addRecord: (record: Omit<PaymentRecord, 'id'>) => void;
    clearHistory: () => void;
}

export const usePaymentStore = create<PaymentState>()(
    persist(
        (set) => ({
            records: [
              // Initial mock data as requested to show understandable UI
              {
                id: 'pay_mock_1',
                planName: 'Sprint Plan',
                amount: 59,
                currency: '₹',
                dateTime: new Date(Date.now() - 86400000).toLocaleString(),
                status: 'Successful'
              },
              {
                id: 'pay_mock_2',
                planName: 'Explorer',
                amount: 0,
                currency: '₹',
                dateTime: new Date(Date.now() - 172800000).toLocaleString(),
                status: 'Successful'
              }
            ],
            addRecord: (record) => set((state) => ({
                records: [
                    { ...record, id: `pay_${Date.now()}` },
                    ...state.records
                ]
            })),
            clearHistory: () => set({ records: [] })
        }),
        {
            name: 'starto-payments-storage',
            storage: createJSONStorage(() => localStorage),
        }
    )
);
