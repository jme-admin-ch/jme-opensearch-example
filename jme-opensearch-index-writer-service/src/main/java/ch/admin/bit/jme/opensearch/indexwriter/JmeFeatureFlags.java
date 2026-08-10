package ch.admin.bit.jme.opensearch.indexwriter;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;

public enum JmeFeatureFlags implements Feature {

    @EnabledByDefault
    @Label("Enable indexing of JmeTransitDecision into OpenSearch")
    JME_TRANSIT_DECISION_INDEXING
}
