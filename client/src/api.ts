import axios from 'axios';

const extractResponseData = (response) => {
  return response.data;
};

export function interpretTransactionInput(transactionId: string, inputIndex: number) {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret`)
    .then(extractResponseData);
}

export function interpretTransactionInputWithSteps(transactionId: string, inputIndex: number, step: number): Promise<string> {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret-with-steps/${step}`)
    .then(extractResponseData);
}

export function fetchTransaction(transactionId) {
  return axios.get(`/api/transaction/${transactionId}`)
    .then(extractResponseData);
}
