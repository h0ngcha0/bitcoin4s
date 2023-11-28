import axios from 'axios'

interface InterpreterResultOut {
  value?: boolean
  type: string
}

interface ScriptElement {
  type: string
  value: boolean
}

interface ScriptExecutionStage {
  type: 'ExecutingScriptSig' | 'ExecutingScriptPubKey' | 'ExecutingScriptP2SH' | 'ExecutingScriptWitness'
}

interface InterpreterStateOut {
  scriptPubKey: ScriptElement[]
  scriptSig: ScriptElement[]
  currentScript: ScriptElement[]
  scriptP2sh: ScriptElement[] | undefined
  scriptWitness: ScriptElement[] | undefined
  scriptWitnessStack: ScriptElement[] | undefined
  stack: ScriptElement[]
  altStack: ScriptElement[]
  stage: ScriptExecutionStage
}

export interface InterpreterOutcome {
  result: InterpreterResultOut
  state: InterpreterStateOut
  step: number | undefined
}

export interface OutPointRaw {
  hash: string
  index: string
}

export interface TxInRaw {
  previousOutput: OutPointRaw
  sigScript: string
  sequence: string
}

export interface TxInsRaw {
  count: string
  txIns: TxInRaw[]
}

export interface TxOutRaw {
  value: string
  pkScript: string
}

export interface TxOutsRaw {
  count: string
  txOuts: TxOutRaw[]
}

export interface TxWitnessRaw {
  witness: string
}

export interface TxWitnessesRaw {
  count: number
  txWitnesses: TxWitnessRaw[]
}

export interface TxRaw {
  version: string
  flag?: string
  txIns: TxInsRaw
  txOuts: TxOutsRaw
  txWitnesses: TxWitnessesRaw[]
  lockTime: string
}

export interface TransactionInput {
  prevHash: string
  outputIndex: number
  script?: string
  parsedScript?: ScriptElement[]
  outputValue: number
  sequence: number
  scriptType: number
  addresses: string[]
  witness?: string[]
}

export interface TransactionOutput {
  value: number
  script: number
  parsedScript?: ScriptElement[]
  spentBy?: string[]
  addresses: string[]
  scriptType: string
}

export interface Transaction {
  hash: string
  hex: string
  txRaw?: TxRaw
  total: number
  size: number
  version: number
  lockTime: number
  inputs: TransactionInput[]
  outputs: TransactionOutput[]
}

function extractResponseData<T>(response: { data: T }): T {
  return response.data
}

export function interpretTransactionInput(transactionId: string, inputIndex: number) {
  return axios.get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret`).then(extractResponseData)
}

export function interpretTransactionInputWithSteps(
  transactionId: string,
  inputIndex: number,
  step: number
): Promise<InterpreterOutcome | undefined> {
  return axios
    .get(`/api/transaction/${transactionId}/input/${inputIndex}/interpret-with-steps/${step}`)
    .then(extractResponseData)
}

export function fetchTransaction(transactionId): Promise<Transaction | undefined> {
  return axios.get(`/api/transaction/${transactionId}`).then(extractResponseData)
}
