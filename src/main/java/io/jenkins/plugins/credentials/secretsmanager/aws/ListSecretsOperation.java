package io.jenkins.plugins.credentials.secretsmanager.aws;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.ListSecretsRequest;
import com.amazonaws.services.secretsmanager.model.ListSecretsResult;
import com.amazonaws.services.secretsmanager.model.SecretListEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Look up all secrets in Secrets Manager using the ListSecrets command. Paginate through secrets
 * until there are none left to get.
 */
public class ListSecretsOperation implements Supplier<List<SecretListEntry>> {

    private final AWSSecretsManager client;

    public ListSecretsOperation(AWSSecretsManager client) {
        this.client = client;
    }

    @Override
    public List<SecretListEntry> get() {
        final List<SecretListEntry> secretList = new ArrayList<>();

        Optional<String> nextToken = Optional.empty();
        do {
            final ListSecretsRequest request = nextToken.map((nt) -> new ListSecretsRequest()
                    .withNextToken(nt))
                    .orElse(new ListSecretsRequest());
            final ListSecretsResult result = client.listSecrets(request);
            final List<SecretListEntry> secrets = result.getSecretList()
                    .stream()
                    .filter(ListSecretsOperation::isNotDeleted)
                    .collect(Collectors.toList());
            secretList.addAll(secrets);
            nextToken = Optional.ofNullable(result.getNextToken());
        } while (nextToken.isPresent());

        return secretList;
    }

    private static boolean isNotDeleted(SecretListEntry entry) {
        return entry.getDeletedDate() == null;
    }
}
