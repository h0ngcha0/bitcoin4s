import axios from 'axios';

const extractResponseData = (response) => {
  return response.data;
};

export function interpretTransactionInput(transactionId, inputIndex) {
  return axios.get(`/transaction/${transactionId}/input/${inputIndex}/interpret`)
    .then(extractResponseData);
}
