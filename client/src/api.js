import axios from 'axios';

const extractResponseData = (response) => {
  return response.data;
};

export function interpretTransactionInput(transactionId, inputIndex) {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret`)
    .then(extractResponseData);
}

export function fetchTransaction(transactionId) {
  return axios.get(`/api/transaction/${transactionId}`)
    .then(extractResponseData);
}
