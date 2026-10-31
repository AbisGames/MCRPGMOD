package com.pipican.stat;

import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanConfig;
import com.pipican.player.PlayerClassData;
import com.pipican.skill.TreeKind;
import com.pipican.skill.TreePoints;
import net.minecraft.core.RegistryAccess;

/**
 * Stat-point arithmetic, shared by the server (which enforces it) and the UIs (which show it).
 * Points are never stored: earned comes from lifetime kill XP ({@link PlayerClassData#statXp()}),
 * spent from points put into stats plus nodes allocated in stat trees.
 *
 * <p>Cumulative XP for the Nth point is {@code first * N + step * N(N-1)/2}, so with the defaults
 * (100, 50) points come at 100, 250, 450, 700... XP.
 */
public final class StatPoints {

    private StatPoints() {}

    /** Cumulative kill XP needed to have earned {@code n} stat points. Saturates at {@link Long#MAX_VALUE}. */
    public static long threshold(long n) {
        if (n <= 0) {
            return 0;
        }
        long first = PipicanConfig.statFirstPointXp();
        long step = PipicanConfig.statPointXpStep();
        try {
            // n(n-1)/2 without overflowing early: halve whichever factor is even.
            long x = n;
            long y = n - 1;
            if ((x & 1L) == 0) {
                x /= 2;
            } else {
                y /= 2;
            }
            return Math.addExact(Math.multiplyExact(first, n), Math.multiplyExact(step, Math.multiplyExact(x, y)));
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    /** Stat points earned with {@code xp} lifetime kill XP. */
    public static int earnedForXp(long xp) {
        if (xp <= 0) {
            return 0;
        }
        long lo = 0;
        long hi = Math.min(xp / PipicanConfig.statFirstPointXp() + 1, Integer.MAX_VALUE);
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (threshold(mid) <= xp) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return (int) lo;
    }

    public static int earned(PlayerClassData data) {
        return earnedForXp(data.statXp());
    }

    /** Kill XP still needed for the next stat point. */
    public static long xpToNextPoint(PlayerClassData data) {
        long target = threshold(earned(data) + 1L);
        return target == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, target - data.statXp());
    }

    /**
     * Points spent: every point put into a stat plus every stat-tree node owned, in every class the
     * player has played (so switching class can't double-spend). Nodes that no longer exist cost nothing.
     */
    public static int spent(RegistryAccess access, PlayerClassData data) {
        int spent = data.totalBoughtStats();
        for (var entry : data.progressByClass().entrySet()) {
            var tree = ClassLookup.rpgClass(access, entry.getKey()).flatMap(c -> ClassLookup.tree(access, c, TreeKind.STAT));
            if (tree.isPresent()) {
                spent += TreePoints.spent(tree.get(), entry.getValue(), TreeKind.STAT);
            }
        }
        return spent;
    }

    /** Unspent stat points. Never negative, even if old saves spent more than they have now. */
    public static int available(RegistryAccess access, PlayerClassData data) {
        return Math.max(0, earned(data) - spent(access, data));
    }
}
