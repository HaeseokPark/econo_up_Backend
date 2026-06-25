package com.econoup.wallet;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import com.econoup.wallet.WalletService.WalletAmountRequest;
import com.econoup.wallet.WalletService.UnlockRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ApiResponse<?> balance(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(walletService.balance(user));
    }

    @PostMapping("/bills/grant")
    public ApiResponse<?> grant(@AuthenticationPrincipal UserEntity user, @RequestBody WalletAmountRequest request) {
        return ApiResponse.ok(walletService.grant(user, request));
    }

    @PostMapping("/bills/spend")
    public ApiResponse<?> spend(@AuthenticationPrincipal UserEntity user, @RequestBody WalletAmountRequest request) {
        return ApiResponse.ok(walletService.spend(user, request));
    }

    @PostMapping("/hearts/refill")
    public ApiResponse<?> refillHeart(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(walletService.refillHeart(user));
    }

    @PostMapping("/hearts/unlimited-pass")
    public ApiResponse<?> unlimitedHeart(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(walletService.purchaseUnlimitedHeart(user));
    }

    @PostMapping("/streak-revive-tickets/purchase")
    public ApiResponse<?> purchaseReviveTicket(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(walletService.purchaseReviveTicket(user));
    }

    @PostMapping("/streak/revive")
    public ApiResponse<?> reviveStreak(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(walletService.reviveStreak(user));
    }

    @GetMapping("/unlocks")
    public ApiResponse<?> unlocks(@AuthenticationPrincipal UserEntity user, @RequestParam String contentType) {
        return ApiResponse.ok(walletService.unlocks(user, contentType));
    }

    @PostMapping("/unlocks/purchase")
    public ApiResponse<?> purchaseUnlock(@AuthenticationPrincipal UserEntity user, @RequestBody UnlockRequest request) {
        return ApiResponse.ok(walletService.purchaseUnlock(user, request));
    }
}
