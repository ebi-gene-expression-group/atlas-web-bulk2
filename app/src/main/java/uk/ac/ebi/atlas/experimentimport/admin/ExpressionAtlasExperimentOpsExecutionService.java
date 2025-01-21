package uk.ac.ebi.atlas.experimentimport.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrud;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.AnalyticsIndexerManager;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.ac.ebi.atlas.experimentimport.admin.Op.LIST;

public class ExpressionAtlasExperimentOpsExecutionService implements ExperimentOpsExecutionService {
    private final ExperimentCrud experimentCrud;
    private final BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader;
    private final AnalyticsIndexerManager analyticsIndexerManager;

    public ExpressionAtlasExperimentOpsExecutionService(ExperimentCrud experimentCrud,
                                                        BaselineCoexpressionProfileLoader
                                                                baselineCoexpressionProfileLoader,
                                                        AnalyticsIndexerManager analyticsIndexerManager) {
        this.experimentCrud = experimentCrud;
        this.baselineCoexpressionProfileLoader = baselineCoexpressionProfileLoader;
        this.analyticsIndexerManager = analyticsIndexerManager;
    }

    @Override
    public List<String> findAllExperiments() {
        return allDtos().map(ExperimentDto::getExperimentAccession).collect(toList());
    }

    @Override
    public Optional<JsonElement> attemptExecuteOneStatelessOp(String accession, Op op) {
        return (op.equals(LIST))
                ? experimentCrud.readExperiment(accession).map(ExperimentDto::toJson)
                : Optional.empty();
    }

    @Override
    public Optional<? extends List<Pair<String, ? extends JsonElement>>>
           attemptExecuteForAllAccessions(Collection<Op> ops) {
        if (ops.equals(Collections.singleton(LIST))) {
            return Optional.of(list());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<? extends List<Pair<String, ? extends JsonElement>>> attemptExecuteForAllAccessions(Op op) {
        if (op.equals(LIST)) {
            return Optional.of(list());
        } else {
            return Optional.empty();
        }
    }

    private Stream<ExperimentDto> allDtos() {
        return experimentCrud.readExperiments().stream();
    }

    private List<Pair<String, ? extends JsonElement>> list() {
        return allDtos()
                .map(experimentDTO -> Pair.of(experimentDTO.getExperimentAccession(), experimentDTO.toJson()))
                .collect(toList());
    }

    @Override
    public JsonPrimitive attemptExecuteStatefulOp(String accession, Op op) {
        JsonPrimitive resultOfTheOp = ExperimentOps.DEFAULT_SUCCESS_RESULT;
        boolean isPrivate = true;
        int deleteCount;
        int loadCount;

        switch (op) {
            case UPDATE_PRIVATE:
                analyticsIndexerManager.deleteFromAnalyticsIndex(accession);
                experimentCrud.updateExperimentPrivate(accession, true);
                break;
            case UPDATE_PUBLIC:
                experimentCrud.updateExperimentPrivate(accession, false);
                break;
            case UPDATE_DESIGN:
                experimentCrud.updateExperimentDesign(accession);
                break;
            case IMPORT_PUBLIC:
                isPrivate = false;
            case IMPORT:
                UUID accessKeyUUID = experimentCrud.createExperiment(accession, isPrivate);
                resultOfTheOp = new JsonPrimitive("Success, access key UUID: " + accessKeyUUID);
                break;
            case DELETE:
                analyticsIndexerManager.deleteFromAnalyticsIndex(accession);
                experimentCrud.deleteExperiment(accession);
                break;
            case COEXPRESSION_UPDATE:
                deleteCount = baselineCoexpressionProfileLoader.deleteCoexpressionsProfile(accession);
                loadCount = baselineCoexpressionProfileLoader.loadBaselineCoexpressionsProfile(accession);
                resultOfTheOp =
                        new JsonPrimitive(
                                String.format(
                                        " deleted %, d and loaded %, d coexpression profiles", deleteCount, loadCount));
                break;
            case COEXPRESSION_IMPORT:
                loadCount = baselineCoexpressionProfileLoader.loadBaselineCoexpressionsProfile(accession);
                resultOfTheOp = new JsonPrimitive(String.format(" loaded %, d coexpression profiles", loadCount));
                break;
            case COEXPRESSION_DELETE:
                deleteCount = baselineCoexpressionProfileLoader.deleteCoexpressionsProfile(accession);
                resultOfTheOp = new JsonPrimitive(String.format(" deleted %, d coexpression profiles", deleteCount));
                break;
            case ANALYTICS_IMPORT:
                loadCount = analyticsIndexerManager.addToAnalyticsIndex(accession);
                resultOfTheOp = new JsonPrimitive(String.format("(re)indexed %, d documents", loadCount));
                break;
            case ANALYTICS_DELETE:
                analyticsIndexerManager.deleteFromAnalyticsIndex(accession);
                break;
            default:
                throw new IllegalArgumentException("Op not supported in Expression Atlas: " + op.name());
        }
        return resultOfTheOp;
    }

}
