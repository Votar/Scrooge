package have

/* Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
* `utxoPool`. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
* constructor.
*/

class TxHandler(utxoPool: UTXOPool) {

    val mUtxoPool: UTXOPool = UTXOPool(utxoPool)

    /**
     * @return true if:
     * (1) all outputs claimed by `tx` are in the current UTXO pool,
     * (2) the signatures on each input of `tx` are valid, +
     * (3) no UTXO is claimed multiple times by `tx`,
     * (4) all of `tx`s output values are non-negative, and +
     * (5) the sum of `tx`s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    fun isValidTx(tx: Transaction): Boolean {
        /*
        * (3) no UTXO is claimed multiple times by `tx`,
        */
        val utxoList = mutableListOf<UTXO>()
        for (i in 0 until tx.numOutputs())
            if (utxoList.contains(UTXO(tx.hash, i)))
                return false
            else
                utxoList.add(UTXO(tx.hash, i))


        /*
        * (1) all outputs claimed by `tx` are in the current UTXO pool,
        */
        utxoList.forEachIndexed { index, output ->
            if (mUtxoPool.contains(output).not())
                return false
        }
        /*
         * (2) the signatures on each input of `tx` are valid,
         */
        for (i in 0 until tx.numInputs())
            if (Crypto.verifySignature(tx.outputs[i].address, tx.getRawDataToSign(i), tx.inputs[i].signature).not())
                return false

        /*
         *(4) all of `tx`s output values are non-negative, and
         */
        var sumOutputs = 0.0
        tx.outputs.forEach { sumOutputs += it.value }
        if (sumOutputs < 0.0)
            return false
        /*
         *(5) the sum of `tx`s input values is greater than or equal to the sum of its output
         * values; and false otherwise.
         */
        var sumInputs = 0.0
        tx.inputs.forEachIndexed { index, input ->
            val prevOutput = mUtxoPool.getTxOutput(UTXO(input.prevTxHash, input.outputIndex))
            sumInputs += prevOutput.value
        }
        if (sumInputs < sumOutputs)
            return false


        return true
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    fun handleTxs(possibleTxs: Array<Transaction>): Array<Transaction> {
        val correctnessTxs = mutableListOf<Transaction>()
        possibleTxs.forEach {
            if (isValidTx(it)) {
                correctnessTxs.add(it)
                it.outputs.forEachIndexed { index, output ->
                    mUtxoPool.addUTXO(UTXO(it.hash, index), output)
                }
            }
        }

        return correctnessTxs.toTypedArray()
    }

}
