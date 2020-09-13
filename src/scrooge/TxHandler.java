package scrooge;

import java.util.ArrayList;
import java.util.HashMap;

import scrooge.Transaction.Input;
import scrooge.Transaction.Output;
import java.security.PublicKey;

public class TxHandler {
	private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
    	// Done
    	this.utxoPool = new scrooge.UTXOPool(utxoPool);
    }

    /**
     * all outputs claimed by {@code tx} are in the current UTXO pool
     * 确保该TX的每一个Input都没有consume掉（在当前的UTXO pool中）
     * @param tx
     * @return
     */
    private boolean notConsumedInput(Transaction tx) {
    	ArrayList<Input> ins = tx.getInputs();
    	for (int i = 0; i < ins.size(); i++) {
    		Transaction.Input in = ins.get(i);
    		byte[] prevTxHash = in.prevTxHash;
    		int outputIndex = in.outputIndex;
    		UTXO utxo = new UTXO(prevTxHash, outputIndex);
    		if (! this.utxoPool.contains(utxo))
    			return false; // 任意一个Input不在当前的UTXO pool中的话，就认为该TX无效
    	}
    	return true;    	
    }
    
    /**
     * the signatures on each input of {@code tx} are valid
     * @param tx
     * @return
     */
    private boolean validSig(Transaction tx) {
    	ArrayList<Input> ins = tx.getInputs();
    	for (int i = 0; i < ins.size(); i++) {
    		Transaction.Input in = ins.get(i); // 当前的in源于前一个TX的某一个out
    		byte[] sig = in.signature;
    		byte[] prevTxHash = in.prevTxHash;
    		int outputIndex = in.outputIndex;
    		UTXO utxo = new UTXO(prevTxHash, outputIndex); // 当前in的源起TX的output，它应该没有被花掉
    		if (! this.utxoPool.contains(utxo))
    			return false; // 任意一个Input不在当前的UTXO pool中的话，就认为该TX无效
    		Transaction.Output out = this.utxoPool.getTxOutput(utxo);
    		PublicKey pk = out.address;
    		if (Crypto.verifySignature(pk, prevTxHash, sig) == false)
    			return false; // 某一个Input的源起TX的Hash的签名，应该和该源起TX对应的output owner的PK相对应
    	}
    	return true;    	
    }
    
    /**
     * no UTXO is claimed multiple times by {@code tx}
     * @param tx
     * @return
     */
    private boolean noDoubleSpending(Transaction tx) {
    	ArrayList<Input> ins = tx.getInputs();
    	ArrayList<UTXO> L = new java.util.ArrayList<UTXO>();
    	for (int i = 0; i < ins.size(); i++) {
    		Transaction.Input in = ins.get(i); // 当前的in源于前一个TX的某一个out
    		byte[] prevTxHash = in.prevTxHash;
    		int outputIndex = in.outputIndex;
    		UTXO utxo = new UTXO(prevTxHash, outputIndex);
    		if (L.contains(utxo))
    			return false;
    		else
    			L.add(utxo);
    	}
    	return true;    	
    }
    
    /**
     * all of {@code tx}s output values are non-negative, and
     * the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     * @param tx
     * @return
     */
    private boolean validAmount(Transaction tx) {
    	ArrayList<Input> ins = tx.getInputs();
    	ArrayList<Output> outs = tx.getOutputs();
    	double total_in = 0;
    	double total_out = 0;
    	for (int i = 0; i < ins.size(); i++) {
    		Transaction.Input in = ins.get(i);
    		byte[] prevTxHash = in.prevTxHash; // 当前的in源于前一个TX的某一个out
    		int outputIndex = in.outputIndex;
    		UTXO utxo = new UTXO(prevTxHash, outputIndex);
    		Transaction.Output out = utxoPool.getTxOutput(utxo);
    		if (out==null)
    			return false;
    		total_in += out.value;	// 收入来源于前面TX的output
    	}
    	
    	if (total_in < 0)	// 总收入不能为负
    		return false;
    	
    	for (int i = 0; i < ins.size(); i++) {
    		Transaction.Output out = outs.get(i); // 遍历所有的out，求出一共要付出多少钱
    		if (out.value < 0) // 支出不能为负
    			return false;
    		total_out += out.value;
    	}
    	
    	if (total_in < total_out) // 总收入不能小于总支出
    		return false;
    	
    	return true;
    }
    
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
    	if (! notConsumedInput(tx))
    		return false;
    	if (! validSig(tx))
    		return false;
    	if (! noDoubleSpending(tx))
    		return false;
    	if (! validAmount(tx))
    		return false;
    	
    	return false;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	// TODO
    	ArrayList<Transaction> validTXs = new ArrayList<Transaction>();
    	// 复制当前的UTXOPool，如果新入的TX在这里面，就从这里面删掉（花掉了）
    	// 如果出现双花问题，则第二个TX在这个tryPool里面就会找不到UTXO
    	UTXOPool tryPool = new UTXOPool(utxoPool); 
    	for (int i = 0; i < possibleTxs.length; i++) {
    		Transaction tx = possibleTxs[i];
    		boolean valid = true;
    		if (! isValidTx(tx))
    			continue; // 直接抛弃无效TX
        	ArrayList<Input> ins = tx.getInputs(); 
        	// 检查该TX中的每一个INPUT，是否有双花问题
        	for (int j = 0; j < ins.size(); j++) {
        		Transaction.Input in = ins.get(j);
        		byte[] prevTxHash = in.prevTxHash;
        		int outputIndex = in.outputIndex;
        		// 针对每一个in，检查是否在当前pool中存在
        		// 如果存在，就在pool中删掉（花掉）
        		UTXO utxo = new UTXO(prevTxHash, outputIndex); 
        		if (tryPool.contains(utxo))
        			tryPool.removeUTXO(utxo); // “花掉”这个UTXO
        		else {
        			//如果有任何一个input所对应的UTXO已经不存在了（不管是之前就不存在还是在这里被删掉了）
        			//我们都需要抛弃掉这个TX
        			valid = false;
        			break;
        		}
        	} // try for a single TX
    		if (valid)
    			validTXs.add(tx);
    	} // iterate each TX
    	Transaction[] result = new Transaction[validTXs.size()];
    	result = validTXs.toArray(result);
    	return result;
    }

}
