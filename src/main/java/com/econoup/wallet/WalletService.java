package com.econoup.wallet;

import com.econoup.common.ApiException;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;

    public WalletService(UserRepository userRepository, PurchaseRepository purchaseRepository) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> balance(UserEntity user) {
        return walletPayload(user);
    }

    @Transactional
    public Map<String, Object> grant(UserEntity user, WalletAmountRequest request) {
        int amount = positiveAmount(request);
        user.billBalance += amount;
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "DEV_GRANT", amount, memo(request)));
        return Map.of(
                "wallet", walletPayload(user),
                "transaction", Map.of("type", "DEV_GRANT", "amount", amount)
        );
    }

    @Transactional
    public Map<String, Object> spend(UserEntity user, WalletAmountRequest request) {
        int amount = positiveAmount(request);
        if (user.billBalance < amount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BILL_NOT_ENOUGH", "Bill balance is not enough.");
        }
        user.billBalance -= amount;
        userRepository.save(user);
        purchaseRepository.save(new PurchaseEntity(user, "SPEND", -amount, memo(request)));
        return Map.of(
                "wallet", walletPayload(user),
                "transaction", Map.of("type", "SPEND", "amount", amount)
        );
    }

    private int positiveAmount(WalletAmountRequest request) {
        if (request == null || request.amount() == null || request.amount() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        return request.amount();
    }

    private String memo(WalletAmountRequest request) {
        return request == null || request.memo() == null ? "" : request.memo();
    }

    private Map<String, Object> walletPayload(UserEntity user) {
        return Map.of(
                "billBalance", user.billBalance,
                "currency", "BILL"
        );
    }

    public record WalletAmountRequest(Integer amount, String memo) {
    }
}
