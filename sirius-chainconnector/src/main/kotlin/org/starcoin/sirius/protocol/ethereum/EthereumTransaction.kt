package org.starcoin.sirius.protocol

import org.apache.commons.lang3.ArrayUtils.isEmpty
import org.ethereum.datasource.MemSizeEstimator.ByteArrayEstimator
import org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY
import org.ethereum.util.ByteUtil.ZERO_BYTE_ARRAY
import java.math.BigInteger
import java.security.SignatureException
import java.util.Arrays
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.ECKey.ECDSASignature
import org.ethereum.crypto.ECKey.MissingPrivateKeyException
import org.ethereum.crypto.HashUtil
import org.ethereum.util.ByteUtil
import org.ethereum.util.RLP
import org.ethereum.util.RLP.encodeElement
import org.ethereum.util.RLPItem
import org.ethereum.util.RLPList
import org.slf4j.LoggerFactory
import org.spongycastle.util.BigIntegers
import org.spongycastle.util.encoders.Hex
import org.starcoin.sirius.core.ChainTransaction

/**
 * A transaction (formally, T) is a single cryptographically
 * signed instruction sent by an actor external to Ethereum.
 * An external actor can be a person (via a mobile device or desktop computer)
 * or could be from a piece of automated software running on a server.
 * There are two types of transactions: those which result in message calls
 * and those which result in the creation of new contracts.
 */
open class EthereumTransaction : ChainTransaction{

    /* SHA3 hash of the RLP encoded transaction */
    private var hash: ByteArray? = null

    /* a counter used to make sure each transaction can only be processed once */
    private var nonce: ByteArray
        set(nonce) {
            this.nonce = nonce
            isParsed = true
        }
        get() {
            rlpParse()
            return if (value == null) ZERO_BYTE_ARRAY else value
        }

    /* the amount of ether to transfer (calculated as wei) */
    private var value: ByteArray
        set(value){
            this.value = value
            isParsed = true
        }
        get(){
            rlpParse()
            return if (value == null) ZERO_BYTE_ARRAY else value
        }
    /* the address of the destination account
        * In creation transaction the receive address is - 0 */
    private var receiveAddress: ByteArray? = null

    /* the amount of ether to pay as a transaction fee
        * to the miner for each unit of gas */
    private var gasPrice: ByteArray? = null

    /* the amount of "gas" to allow for the computation.
        * Gas is the fuel of the computational engine;
        * every computational step taken and every byte added
        * to the state or transaction list consumes some gas. */
    private var gasLimit: ByteArray? = null

    /* An unlimited size byte array specifying
        * input [data] of the message call or
        * Initialization code for a new contract */
    private var data: ByteArray? = null
    private var chainId: Int? = null

    /* the elliptic curve signature
        * (including public key recovery bits) */
    private var signature: ECDSASignature? = null

    protected var sendAddress: ByteArray? = null

    /* Tx in encoded form */
    protected var rlpEncoded: ByteArray? = null
    private var rawHash: ByteArray? = null
    /* Indicates if this transaction has been parsed
        * from the RLP-encoded data */
    var isParsed = false
        private set

    val isValueTx: Boolean
        get() {
            rlpParse()
            return value != null
        }

    val contractAddress: ByteArray?
        get() {
            return if (!isContractCreation) null else HashUtil.calcNewAddr(this.sender, nonce)
        }

    val isContractCreation: Boolean
        get() {
            rlpParse()
            return this.receiveAddress == null || Arrays.equals(this.receiveAddress, ByteUtil.EMPTY_BYTE_ARRAY)
        }

    /*
        * Crypto
        */

    open val key: ECKey?
        get() {
            val hash = getRawHash()
            return ECKey.recoverFromSignature(signature!!.v.toInt(), signature!!, hash)
        }

    open val sender: ByteArray?
        @Synchronized get() {
            try {
                if (sendAddress == null && getSignature() != null) {
                    sendAddress = ECKey.signatureToAddress(getRawHash(), getSignature()!!)
                }
                return sendAddress
            } catch (e: SignatureException) {
                logger.error(e.message, e)
            }

            return null
        }

    /**
     * For signatures you have to keep also
     * RLP of the transaction without any signature data
     */
    open// parse null as 0 for nonce
    // Since EIP-155 use chainId for v
    val encodedRaw: ByteArray
        get() {

            rlpParse()
            val rlpRaw: ByteArray
            var nonce: ByteArray? = null
            nonce = if (this.nonce == null || this.nonce!!.size == 1 && this.nonce!![0].toInt() == 0) {
                encodeElement(null)
            } else {
                encodeElement(this.nonce)
            }
            val gasPrice = encodeElement(this.gasPrice)
            val gasLimit = encodeElement(this.gasLimit)
            val receiveAddress = encodeElement(this.receiveAddress)
            val value = encodeElement(this.value)
            val data = encodeElement(this.data)
            if (chainId == null) {
                rlpRaw = RLP.encodeList(
                    nonce, gasPrice, gasLimit, receiveAddress,
                    value, data
                )
            } else {
                val v: ByteArray
                val r: ByteArray
                val s: ByteArray
                v = RLP.encodeInt(chainId!!)
                r = encodeElement(EMPTY_BYTE_ARRAY)
                s = encodeElement(EMPTY_BYTE_ARRAY)
                rlpRaw = RLP.encodeList(
                    nonce, gasPrice, gasLimit, receiveAddress,
                    value, data, v, r, s
                )
            }
            return rlpRaw
        }

    open// parse null as 0 for nonce
    // Since EIP-155 use chainId for v
    val encoded: ByteArray?
        get() {

            if (rlpEncoded != null) return rlpEncoded
            var nonce: ByteArray? = null
            if (this.nonce == null || this.nonce!!.size == 1 && this.nonce!![0].toInt() == 0) {
                nonce = encodeElement(null)
            } else {
                nonce = encodeElement(this.nonce)
            }
            val gasPrice = encodeElement(this.gasPrice)
            val gasLimit = encodeElement(this.gasLimit)
            val receiveAddress = encodeElement(this.receiveAddress)
            val value = encodeElement(this.value)
            val data = encodeElement(this.data)

            val v: ByteArray
            val r: ByteArray
            val s: ByteArray

            if (signature != null) {
                var encodeV: Int
                if (chainId == null) {
                    encodeV = signature!!.v.toInt()
                } else {
                    encodeV = signature!!.v - LOWER_REAL_V
                    encodeV += chainId!! * 2 + CHAIN_ID_INC
                }
                v = RLP.encodeInt(encodeV)
                r = encodeElement(BigIntegers.asUnsignedByteArray(signature!!.r))
                s = encodeElement(BigIntegers.asUnsignedByteArray(signature!!.s))
            } else {
                v = if (chainId == null) encodeElement(EMPTY_BYTE_ARRAY) else RLP.encodeInt(chainId!!)
                r = encodeElement(EMPTY_BYTE_ARRAY)
                s = encodeElement(EMPTY_BYTE_ARRAY)
            }

            this.rlpEncoded = RLP.encodeList(
                nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s
            )

            this.hash = HashUtil.sha3(rlpEncoded)

            return rlpEncoded
        }

    constructor(rawData: ByteArray) {
        this.rlpEncoded = rawData
        isParsed = false
    }

    @JvmOverloads
    constructor(
        nonce: ByteArray,
        gasPrice: ByteArray,
        gasLimit: ByteArray,
        receiveAddress: ByteArray?,
        value: ByteArray,
        data: ByteArray?,
        chainId: Int? = null
    ) {
        this.nonce = nonce
        this.gasPrice = gasPrice
        this.gasLimit = gasLimit
        this.receiveAddress = receiveAddress
        if (ByteUtil.isSingleZero(value)) {
            this.value = EMPTY_BYTE_ARRAY
        } else {
            this.value = value
        }
        this.data = data
        this.chainId = chainId

        if (receiveAddress == null) {
            this.receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY
        }

        isParsed = true
    }

    @JvmOverloads
    constructor(
        nonce: ByteArray,
        gasPrice: ByteArray,
        gasLimit: ByteArray,
        receiveAddress: ByteArray,
        value: ByteArray,
        data: ByteArray,
        r: ByteArray,
        s: ByteArray,
        v: Byte,
        chainId: Int? = null
    ) : this(nonce, gasPrice, gasLimit, receiveAddress, value, data, chainId) {
        this.signature = ECDSASignature.fromComponents(r, s, v)
    }


    private fun extractChainIdFromRawSignature(bv: BigInteger, r: ByteArray?, s: ByteArray?): Int? {
        if (r == null && s == null) return bv.toInt()  // EIP 86
        if (bv.bitLength() > 31) return Integer.MAX_VALUE // chainId is limited to 31 bits, longer are not valid for now
        val v = bv.toLong()
        return if (v == LOWER_REAL_V.toLong() || v == (LOWER_REAL_V + 1).toLong()) null else ((v - CHAIN_ID_INC) / 2).toInt()
    }

    private fun getRealV(bv: BigInteger): Byte {
        if (bv.bitLength() > 31) return 0 // chainId is limited to 31 bits, longer are not valid for now
        val v = bv.toLong()
        if (v == LOWER_REAL_V.toLong() || v == (LOWER_REAL_V + 1).toLong()) return v.toByte()
        val realV = LOWER_REAL_V.toByte()
        var inc = 0
        if (v.toInt() % 2 == 0) inc = 1
        return (realV + inc).toByte()
    }

    @Synchronized
    fun verifier() {
        rlpParse()
        validate()
    }

    @Synchronized
    open fun rlpParse() {
        if (isParsed) return
        try {
            val decodedTxList = RLP.decode2(rlpEncoded!!)
            val transaction = decodedTxList[0] as RLPList

            // Basic verification
            if (transaction.size > 9) throw RuntimeException("Too many RLP elements")
            for (rlpElement in transaction) {
                if (rlpElement !is RLPItem)
                    throw RuntimeException("Transaction RLP elements shouldn't be lists")
            }

            this.nonce = transaction[0].rlpData
            this.gasPrice = transaction[1].rlpData
            this.gasLimit = transaction[2].rlpData
            this.receiveAddress = transaction[3].rlpData
            this.value = transaction[4].rlpData
            this.data = transaction[5].rlpData
            // only parse signature in case tx is signed
            if (transaction[6].rlpData != null) {
                val vData = transaction[6].rlpData
                val v = ByteUtil.bytesToBigInteger(vData)
                val r = transaction[7].rlpData
                val s = transaction[8].rlpData
                this.chainId = extractChainIdFromRawSignature(v, r, s)
                if (r != null && s != null) {
                    this.signature = ECDSASignature.fromComponents(r!!, s!!, getRealV(v))
                }
            } else {
                logger.debug("RLP encoded tx is not signed!")
            }
            this.hash = HashUtil.sha3(rlpEncoded)
            this.isParsed = true
        } catch (e: Exception) {
            throw RuntimeException("Error on parsing RLP", e)
        }

    }

    private fun validate() {
        if (nonce.size > HASH_LENGTH) throw RuntimeException("Nonce is not valid")
        if (receiveAddress != null && receiveAddress!!.size != 0 && receiveAddress!!.size != ADDRESS_LENGTH)
            throw RuntimeException("Receive address is not valid")
        if (gasLimit!!.size > HASH_LENGTH)
            throw RuntimeException("Gas Limit is not valid")
        if (gasPrice != null && gasPrice!!.size > HASH_LENGTH)
            throw RuntimeException("Gas Price is not valid")
        if (value != null && value!!.size > HASH_LENGTH)
            throw RuntimeException("Value is not valid")
        if (getSignature() != null) {
            if (BigIntegers.asUnsignedByteArray(signature!!.r).size > HASH_LENGTH)
                throw RuntimeException("Signature R is not valid")
            if (BigIntegers.asUnsignedByteArray(signature!!.s).size > HASH_LENGTH)
                throw RuntimeException("Signature S is not valid")
            if (sender != null && sender!!.size != ADDRESS_LENGTH)
                throw RuntimeException("Sender is not valid")
        }
    }

    fun getHash(): ByteArray? {
        if (!isEmpty(hash)) return hash
        rlpParse()
        encoded
        return hash
    }

    fun getRawHash(): ByteArray? {
        rlpParse()
        if (rawHash != null) return rawHash
        val plainMsg = this.encodedRaw
        rawHash = HashUtil.sha3(plainMsg)
        return rawHash
    }

    fun getReceiveAddress(): ByteArray? {
        rlpParse()
        return receiveAddress
    }

    protected fun setReceiveAddress(receiveAddress: ByteArray) {
        this.receiveAddress = receiveAddress
        isParsed = true
    }

    fun getGasPrice(): ByteArray {
        rlpParse()
        return if (gasPrice == null) ZERO_BYTE_ARRAY else gasPrice!!
    }

    protected fun setGasPrice(gasPrice: ByteArray) {
        this.gasPrice = gasPrice
        isParsed = true
    }

    fun getGasLimit(): ByteArray {
        rlpParse()
        return if (gasLimit == null) ZERO_BYTE_ARRAY else gasLimit!!
    }

    protected fun setGasLimit(gasLimit: ByteArray) {
        this.gasLimit = gasLimit
        isParsed = true
    }

    fun nonZeroDataBytes(): Long {
        if (data == null) return 0
        var counter = 0
        for (aData in data!!) {
            if (aData.toInt() != 0) ++counter
        }
        return counter.toLong()
    }

    fun zeroDataBytes(): Long {
        if (data == null) return 0
        var counter = 0
        for (aData in data!!) {
            if (aData.toInt() == 0) ++counter
        }
        return counter.toLong()
    }


    fun getData(): ByteArray? {
        rlpParse()
        return data
    }

    protected fun setData(data: ByteArray) {
        this.data = data
        isParsed = true
    }

    fun getSignature(): ECDSASignature? {
        rlpParse()
        return signature
    }

    fun getChainId(): Int? {
        rlpParse()
        return if (chainId == null) null else chainId as Int
    }


    @Deprecated("should prefer #sign(ECKey) over this method")
    @Throws(MissingPrivateKeyException::class)
    open fun sign(privKeyBytes: ByteArray) {
        sign(ECKey.fromPrivate(privKeyBytes))
    }

    @Throws(MissingPrivateKeyException::class)
    fun sign(key: ECKey) {
        this.signature = key.sign(this.getRawHash())
        this.rlpEncoded = null
    }

    override fun toString(): String {
        return toString(Integer.MAX_VALUE)
    }

    fun toString(maxDataSize: Int): String {
        rlpParse()
        val dataS: String
        if (data == null) {
            dataS = ""
        } else if (data!!.size < maxDataSize) {
            dataS = ByteUtil.toHexString(data)
        } else {
            dataS = ByteUtil.toHexString(Arrays.copyOfRange(data!!, 0, maxDataSize)) +
                    "... (" + data!!.size + " bytes)"
        }
        return "TransactionData [" + "hash=" + ByteUtil.toHexString(hash) +
                "  nonce=" + ByteUtil.toHexString(nonce) +
                ", gasPrice=" + ByteUtil.toHexString(gasPrice) +
                ", gas=" + ByteUtil.toHexString(gasLimit) +
                ", receiveAddress=" + ByteUtil.toHexString(receiveAddress) +
                ", sendAddress=" + ByteUtil.toHexString(sender) +
                ", value=" + ByteUtil.toHexString(value) +
                ", data=" + dataS +
                ", signatureV=" + (if (signature == null) "" else signature!!.v) +
                ", signatureR=" + (if (signature == null) "" else ByteUtil.toHexString(
            BigIntegers.asUnsignedByteArray(
                signature!!.r
            )
        )) +
                ", signatureS=" + (if (signature == null) "" else ByteUtil.toHexString(
            BigIntegers.asUnsignedByteArray(
                signature!!.s
            )
        )) +
                "]"
    }

    override fun hashCode(): Int {

        val hash = this.getHash()
        var hashCode = 0

        for (i in hash!!.indices) {
            hashCode += hash!![i] * i
        }

        return hashCode
    }

    override fun equals(obj: Any?): Boolean {

        if (obj !is EthereumTransaction) return false
        val tx = obj as EthereumTransaction?

        return tx!!.hashCode() == this.hashCode()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EthereumTransaction::class.java!!)
        private val DEFAULT_GAS_PRICE = BigInteger("10000000000000")
        private val DEFAULT_BALANCE_GAS = BigInteger("21000")

        val HASH_LENGTH = 32
        val ADDRESS_LENGTH = 20

        /**
         * Since EIP-155, we could encode chainId in V
         */
        private val CHAIN_ID_INC = 35
        private val LOWER_REAL_V = 27


        fun createDefault(to: String, amount: BigInteger, nonce: BigInteger, chainId: Int?): EthereumTransaction {
            return create(to, amount, nonce, DEFAULT_GAS_PRICE, DEFAULT_BALANCE_GAS, chainId)
        }


        fun create(
            to: String, amount: BigInteger, nonce: BigInteger, gasPrice: BigInteger,
            gasLimit: BigInteger, chainId: Int?
        ): EthereumTransaction {
            return EthereumTransaction(
                BigIntegers.asUnsignedByteArray(nonce),
                BigIntegers.asUnsignedByteArray(gasPrice),
                BigIntegers.asUnsignedByteArray(gasLimit),
                Hex.decode(to),
                BigIntegers.asUnsignedByteArray(amount), null,
                chainId
            )
        }

        val MemEstimator  = { tx: EthereumTransaction ->
            ByteArrayEstimator.estimateSize(tx.hash) +
                    ByteArrayEstimator.estimateSize(tx.nonce) +
                    ByteArrayEstimator.estimateSize(tx.value) +
                    ByteArrayEstimator.estimateSize(tx.gasPrice) +
                    ByteArrayEstimator.estimateSize(tx.gasLimit) +
                    ByteArrayEstimator.estimateSize(tx.data) +
                    ByteArrayEstimator.estimateSize(tx.sendAddress) +
                    ByteArrayEstimator.estimateSize(tx.rlpEncoded) +
                    ByteArrayEstimator.estimateSize(tx.rawHash) +
                    (if (tx.chainId != null) 24 else 0).toLong() +
                    (if (tx.signature != null) 208 else 0).toLong() + // approximate size of signature

                    16
        } // Object header + ref
    }
}
/**
 * Warning: this transaction would not be protected by replay-attack protection mechanism
 * Use [Transaction.Transaction] constructor instead
 * and specify the desired chainID
 */
/**
 * Warning: this transaction would not be protected by replay-attack protection mechanism
 * Use [Transaction.Transaction]
 * constructor instead and specify the desired chainID
 */
