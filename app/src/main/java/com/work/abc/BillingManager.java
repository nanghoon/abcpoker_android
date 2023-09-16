package com.work.abc;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    //각 주요 결재 변수들을 선언해 줍니다.
    private String TAG = "BILLING";
    private BillingClient billingClient;
    private List<SkuDetails> skuDetails_list;
    private ConsumeResponseListener consumeResponseListener;
    private Context context;

    private String itemNm;
    private String itemMoney;

    //초기화 선언
    public BillingManager(Context context) {
        this.context = context;
        billingClient = BillingClient.newBuilder(context)
                .setListener(this::onPurchasesUpdated)//구매시 구매완료 및 실패에 대한 콜백을 해주는 부분입니다.
                .enablePendingPurchases()
                .build();

        // 결제 이벤트를 실행했을때, 결제 접속하는 과정이라 보시면 됩니다.
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) { // 접속 완료
                    Log.e(TAG, "biilingcount2");
                    getSkuDetailList(); // 접속 후 구매목록 가져오기
                }
            }

            @Override
            public void onBillingServiceDisconnected() { //이건 연결이 해제되거나, 연결되지 안았을때 이벤트 입니다.
                // 본인이 원하는 이벤트나 오류 표출문구를 넣어주세요.
            }
        });

        // 상품 소모결과 리스너 (상품 구매시 실행되는 함수)
        //상품을 구매하거나 하면 purchaseToken에 값이 저장이 되는데 그것을 보여줍니다.
        consumeResponseListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "상품을 성공적으로 소모하였습니다. 소모된 상품 => " + purchaseToken);
                    return;
                } else {
                    Log.e(TAG, "상품 소모에 실패하였습니다. 오류코드 (" + billingResult.getResponseCode() + "), 대상 상품 코드: " + purchaseToken);
                    return;
                }
            }
        };
    }
// 처음 BillingService를 초기화 하면, 구글창에 연결을 하고 구매리스트를 변수에저장합니다.
// 그리고 현재 무언가 구매한 상태라면 그 마지막 구매 목록을 불러와 보여줍니다


    //결제과 완료 됐을때
    //윗부분 billingClient.setListner(this::onPurchaseUpdated) 에 사용되는 함수입니다.
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        Log.e(TAG,"biilingcount1 RESPONSECODE : " + billingResult.getResponseCode() + " , : " + purchases);
        Log.e(TAG, " BillingClient responcode : " + BillingClient.BillingResponseCode.DEVELOPER_ERROR);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.e(TAG, "결제에 성공했으며, 아래에 구매한 상품들이 나열됨");
            for (Purchase purchase : purchases) {
                Log.e(TAG, "결제 구매완료상품: " + purchases);
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }


    //구매 목록을 변수에 저장하고, 실제 구글상품 목록을 가져와 비교합니다.
    //이때 skuList.add 에는 플레이스토어 콘설에서 등록한 결제 상품의 id를 입력해주시면 됩니다.
    public void getSkuDetailList() {
        Log.e(TAG, "biilingcount3");
        List<String> skuList = new ArrayList<>();
        skuList.add("c_0");
        skuList.add("c_1");
        skuList.add("c_2");
        skuList.add("c_3");
        skuList.add("c_4");
        skuList.add("c_5");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);//.INAPP은 일회용 결제고, SUBS는 구독형 결제에요~
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        Log.e(TAG, "biilingcount3.1");
                        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            Log.e(TAG, "biilingcount3.2");
                            return;
                        }
                        Log.e(TAG, "biilingcount3.3");
                        //상품정보를 가저오지 못함 면 동작 이벤트
                        if (skuDetailsList == null) {
                            Log.e(TAG,"결제 상품 리스트에 없음 ");
                            return;
                        }

                        //상품사이즈 체크
                        Log.e(TAG, "결제 상품 리스트 크기 : " + skuDetailsList.size());

                        try {
                            for (SkuDetails skuDetails : skuDetailsList) {
                                String title = skuDetails.getTitle();
                                String sku = skuDetails.getSku();
                                String price = skuDetails.getPrice();

                            }
                        } catch (Exception e) {
                            Log.e(TAG, "itemerror" + e.toString());
                        }

                        skuDetails_list = skuDetailsList;
                    }

                });
    }


    //인자중 itemid 는 구매 품목 id를 문자열로, Activity 는 현재 액티비티를 넣어주면 됩니다.
    //이 Activity인자는 구매를 성공했는지 아닌지를 값을 반환해 주기 위해 있는 인자이므로 크게 신경을 안써도 됩니다.
    public void purchase(Activity activity, String itemid) {
        Log.d(TAG , " purchase call :" + itemid + " skuDetails_list:" + skuDetails_list.size());
        SkuDetails skuDetails = null;
        if(null != skuDetails_list){
            for(int i=0; i<skuDetails_list.size(); i++){
                SkuDetails skuinfo = skuDetails_list.get(i);
                if(skuinfo.getSku().equals(itemid)){//해당 상품을 상품목록에 있는지 비교하고 있으면 다음으로 넘어갑니다.
                    skuDetails = skuinfo;
                    break;
                }
            }
            Log.e(TAG,"biilingcount4 skuDetail : " + skuDetails);
            if(skuDetails != null){
                itemNm = itemid;
                itemMoney = skuDetails.getPrice();
            }
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails) //구매 flow에 해당 목록id가 저장된 변수를 넣습니다.
                    .build();
            Log.e(TAG,"purchase state : "+billingClient.launchBillingFlow(activity,flowParams).getResponseCode());
            // 아까 말했다시피 단지 성공여부를 알려주는 값입니다.  Activity는 그냥 현재, 결제하고 있는 Activity를 입력하시면 됩니다.
        }

    }



    // 이부분은 구매를 하는 함수입니다.
    // 버튼클릭하면 구매를 원할시 버튼이벤트에 이 함수를 넣어주면 됩니다.
    // 위에 선언된 purchase를 이 함수의 인자에 넣어주고 함수를 실행하면 됩니다.
    // 그럼 구매, 토큰값 구매자에게 적용 등등 작업을하여 구매 완료를 이끌어 냅니다.
    public void handlePurchase(Purchase purchase) {
        Log.e(TAG,"biilingcount5");
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        //구매가 되면 위에서 처럼 ComsumeResponesListner을 가진 변수로 결제 성공 후 이벤트를 등록합니다.
        //결제상태를 계속 체크하면서, 장애, 정보등을 전당해 줍니다.
        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                    ((MainActivity)MainActivity.mainContext).sendResult(itemNm,itemMoney);
                    Log.e(TAG,"완료...전달 : " + itemNm + " , " + itemMoney);

                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

}
