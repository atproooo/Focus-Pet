package com.example.focuspet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class PetExpressionSelector {
    static PetExpression choose(PetExpression baseExpression, Set<String> enabledKeys,
                                int context, Random random) {
        List<PetExpression> candidates = new ArrayList<>();
        if (enabledKeys != null) {
            for (String key : enabledKeys) {
                PetExpression expression = PetExpression.fromKey(key);
                if (expression != null && expression.learnable
                        && expression.supportsContext(context)) {
                    candidates.add(expression);
                }
            }
        }
        if (candidates.isEmpty()) return baseExpression;
        if (baseExpression == PetExpression.NEUTRAL || random.nextBoolean()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return baseExpression;
    }

    private PetExpressionSelector() { }
}
