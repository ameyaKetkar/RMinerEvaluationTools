package refdiff.evaluation.db.model;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DbCommitResultDao extends PagingAndSortingRepository<DbCommitResult, Integer> {

    DbCommitResult findOneByCommitAndTool(DbCommit commit, String tool);

}
