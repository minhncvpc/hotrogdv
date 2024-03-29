import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import com.google.gson.GsonBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class NCM_BlockChain {

    public static ArrayList<VNPT_Minh> blockchain = new ArrayList<VNPT_Minh>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA ;
    //Số lượng điện thoại kho 1
    public static Wallet walletB; //Số lượng điện thoại kho 2
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider

        //Create wallets:
        walletA = new Wallet();
        walletB = new Wallet();
        Wallet coinbase = new Wallet();
        Transaction sendFund;
        Scanner sc = new Scanner(System.in);
        System.out.println("Nhập SL điện thoại tồn tại kho 1: ");
        float kho1 =sc.nextFloat();



        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, kho1, null);
        genesisTransaction.generateSignature(coinbase.privateKey);//Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        VNPT_Minh genesis = new VNPT_Minh("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Số điện thoại trong kho 1 là : " + walletA.getBalance());

        // Nhap thong tin khoi tao kho 2
        System.out.print("Nhap so luong dien thoai trong kho 2: ");
        float initBalanceB = sc.nextFloat();

        //Khởi tạo giao dịch gốc, để chuyển điện thoại vào kho 2
        genesisTransaction = new Transaction(coinbase.publicKey, walletB.publicKey, initBalanceB, null);
        genesisTransaction.generateSignature(coinbase.privateKey);     //Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Số điện thoại của kho 2 là : " + walletB.getBalance());

        VNPT_Minh block1 = new VNPT_Minh(genesis.hash);

        //Kiểm tra số lượng chuyển thoả mãn yêu cầu
        boolean fail = true;
        while (fail){
            System.out.print("Nhap so luong dien thoai can chuyen tu kho 1 den kho 2: ");
            float numberTransfer = sc.nextFloat();
            System.out.println("Dang xu ly .............................................");
            sendFund = walletA.sendFunds(walletB.publicKey, numberTransfer);
            if (sendFund==null){
                continue;
            }else{
                fail= false;
                block1.addTransaction(sendFund);
            }
        }
        addBlock(block1);
        System.out.println("Ket qua so dien thoai moi trong cac kho sau khi chuyen: ");
        System.out.println("Số điện thoại trong kho 1 là : " + walletA.getBalance());
        System.out.println("Số điện thoại trong kho 2 là : " + walletB.getBalance());
    }
    public static Boolean isChainValid() {
        VNPT_Minh currentBlock;
        VNPT_Minh previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }

            }

        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_Minh newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}

