package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DomainRepository {

    private final Map<SearchItemIdentifier, SearchItemContainer> searchItems = new ConcurrentHashMap<>();

    public Optional<SearchItemContainer> findSearchItem(String indexType, String originId, String originVersion) {
        return Optional.ofNullable(searchItems.get(new SearchItemIdentifier(indexType, originId, originVersion)));
    }

    public void saveSearchItemContainer(String indexType, String originId, String originVersion,
                                        SearchItemContainer searchItemContainer) {
        searchItems.put(new SearchItemIdentifier(indexType, originId, originVersion), searchItemContainer);
    }

    private record SearchItemIdentifier(String indexType,
                                        String originId,
                                        String originVersion) {

    }

}
