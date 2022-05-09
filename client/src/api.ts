import axios from 'axios';

interface Result {
  value: boolean
}
//type InterpreterResultOut = 'NoResult' | Result;
type InterpreterResultOut = Result;

interface ScriptElement {
  type: string,
  value: boolean
}

type ScriptExecutionStage = 'ExecutingScriptSig' | 'ExecutingScriptPubKey' | 'ExecutingScriptP2SH' | 'ExecutingScriptWitness';

interface InterpreterStateOut {
  scriptPubKey: ScriptElement[],
  scriptSig: ScriptElement[],
  currentScript: ScriptElement[],
  scriptP2sh: ScriptElement[] | undefined,
  scriptWitness: ScriptElement[] | undefined,
  scriptWitnessStack: ScriptElement[] | undefined,
  stack: ScriptElement[],
  altStack: ScriptElement[],
  stage: ScriptExecutionStage
}

export interface InterpreterOutcome {
  result: InterpreterResultOut,
  state: InterpreterStateOut,
  step: number | undefined
}

function extractResponseData<T>(response: { data: T }): T {
  return response.data;
}

export function interpretTransactionInput(transactionId: string, inputIndex: number) {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret`)
    .then(extractResponseData);
}

export function interpretTransactionInputWithSteps(transactionId: string, inputIndex: number, step: number): Promise<InterpreterOutcome | undefined> {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret-with-steps/${step}`)
    .then(extractResponseData);
}

export function fetchTransaction(transactionId) {
  return axios.get(`/api/transaction/${transactionId}`)
    .then(extractResponseData);
}
