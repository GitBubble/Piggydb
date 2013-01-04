package marubinotto.piggydb.impl.query;

import static marubinotto.piggydb.impl.QueryUtils.appendLimit;
import static marubinotto.util.CollectionUtils.joinToString;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import marubinotto.piggydb.impl.H2FragmentRepository;
import marubinotto.piggydb.impl.QueryUtils;
import marubinotto.piggydb.impl.mapper.FragmentRowMapper;
import marubinotto.piggydb.model.Fragment;
import marubinotto.piggydb.model.FragmentList;
import marubinotto.piggydb.model.base.Repository;
import marubinotto.piggydb.model.entity.RawFragment;
import marubinotto.piggydb.model.query.FragmentsByIds;
import marubinotto.piggydb.model.query.FragmentsQuery;
import marubinotto.piggydb.model.query.FragmentsSortOption;
import marubinotto.util.Assert;
import marubinotto.util.CollectionUtils;
import marubinotto.util.paging.Page;
import marubinotto.util.paging.PageUtils;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class H2FragmentsQueryBase implements FragmentsQuery {
	
	private H2FragmentRepository repository;
	
	public void setRepository(Repository<Fragment> repository) {
		Assert.Arg.notNull(repository, "repository");
		this.repository = (H2FragmentRepository)repository;
	}
	
	public H2FragmentRepository getRepository() {
		return this.repository;
	}
	
	public JdbcTemplate getJdbcTemplate() {
		return getRepository().getJdbcTemplate();
	}

	public FragmentRowMapper getRowMapper() {
		return getRepository().getFragmentRowMapper();
	}
	
	protected FragmentsQuery getDelegateeQuery(Class<? extends FragmentsQuery> queryType) 
	throws Exception {
		FragmentsQuery query = (FragmentsQuery)getRepository().getQuery(queryType);
		query.setSortOption(getSortOption());
		query.setEagerFetching(isEagerFetching());
		query.setEagerFetchingMore(isEagerFetchingMore());
		return query;
	}
	
	protected List<Fragment> getByIds(Collection<Long> fragmentIds) throws Exception {
		Assert.Arg.notNull(fragmentIds, "fragmentIds");
		FragmentsByIds query = (FragmentsByIds)getDelegateeQuery(FragmentsByIds.class);
		query.setIds(fragmentIds);
		return query.getAll();
	}
	
	
	// -----
	
	private FragmentsSortOption sortOption = new FragmentsSortOption();
	
	public void setSortOption(FragmentsSortOption sortOption) {
		Assert.Arg.notNull(sortOption, "sortOption");
		this.sortOption = sortOption;
	}
	
	public FragmentsSortOption getSortOption() {
		return this.sortOption;
	}
	
	// -----

	private boolean eagerFetching = false;
	private boolean eagerFetchingMore = false;
	
	public void setEagerFetching(boolean eagerFetching) {
		this.eagerFetching = eagerFetching;
	}
	
	public void setEagerFetchingMore(boolean eagerFetchingMore) {
		this.eagerFetchingMore = eagerFetchingMore;
	}
	
	public boolean isEagerFetching() {
		return this.eagerFetching;
	}

	public boolean isEagerFetchingMore() {
		return this.eagerFetchingMore;
	}

	private void eagerFetch(List<RawFragment> fragments) throws Exception {
		if (fragments.isEmpty()) return;
		
		if (this.eagerFetching) {
			getRepository().refreshClassifications(fragments);
			getRepository().setParentsAndChildrenWithGrandchildrenToEach(fragments);
			
			if (this.eagerFetchingMore) {
				FragmentList<RawFragment> children = 
					new FragmentList<RawFragment>(fragments).getChildren();
				getRepository().refreshClassifications(children.getFragments());
				getRepository().setParentsToEach(children);
				for (RawFragment child : children) child.checkTwoWayRelations();
			}
		}
	}
	
	// -----
	
	private StringBuilder sql;
	private List<Object> sqlArgs;
	private String fromWhere;
	
	private void buildSelectFromWhere() throws Exception {
		this.sql = new StringBuilder();
		this.sqlArgs = new ArrayList<Object>();
		
		appendSelect(this.sql);
		
		StringBuilder fromWhere = new StringBuilder();
		appendFromWhere(fromWhere, this.sqlArgs);
		this.fromWhere = fromWhere.toString();
		this.sql.append(" " + this.fromWhere);
	}
		
	public List<Fragment> getAll() throws Exception {
		buildSelectFromWhere();
		appendSortOption(this.sql, getRowMapper().getColumnPrefix());
		
		List<RawFragment> results = getRepository().query(this.sql.toString(), this.sqlArgs.toArray());
		
		eagerFetch(results);
		
		return CollectionUtils.<Fragment>covariantCast(results);
	}
	
	public Page<Fragment> getPage(int pageSize, int pageIndex) throws Exception {
		buildSelectFromWhere();
		appendSortOption(this.sql, getRowMapper().getColumnPrefix());
		appendLimit(this.sql, pageSize, pageIndex);
		
		List<RawFragment> results = getRepository().query(this.sql.toString(), this.sqlArgs.toArray());
		
		eagerFetch(results);
		
		return PageUtils.<Fragment>covariantCast(
			PageUtils.toPage(results, pageSize, pageIndex, getTotalCounter()));
	}

	protected void appendSelect(StringBuilder sql) {
		sql.append("select ");
		sql.append(getRowMapper().selectAll());
		
		// append a field to sort by a string field with case ignored
		if (this.sortOption.orderBy.isString() && !this.sortOption.shuffle) {
			sql.append(", ");
			sql.append(ignoreCaseForSort(
				this.sortOption.orderBy.getName(), getRowMapper().getColumnPrefix()));
		}
	}
	
	protected abstract void appendFromWhere(StringBuilder sql, List<Object> args) 
	throws Exception;
	
	protected void appendSortOption(StringBuilder sql, String columnPrefix) {
		sql.append(" order by ");
		
		// shuffle
		if (this.sortOption.shuffle) {
			sql.append("rand()");
			return;
		}

		// sort field: [default] the column name with the prefix
		//             [string column] the alias name for upper-cased value in the select
		if (this.sortOption.orderBy.isString())
			sql.append("ns_" + this.sortOption.orderBy.getName());
		else
			sql.append(defaultIfNull(columnPrefix, "") + this.sortOption.orderBy.getName());
		
		// asc/desc
		if (this.sortOption.ascending)
			sql.append(" nulls last");
		else
			sql.append(" desc nulls first");
	}
	
	protected static String ignoreCaseForSort(String columnName, String prefix) {
		return "UPPER(" + prefix + columnName + ") as ns_" + columnName;
	}
	
	protected PageUtils.TotalCounter getTotalCounter() {
		final String countSql = "select count(*) " + this.fromWhere;
		final Object[] args = this.sqlArgs.toArray();
		return new PageUtils.TotalCounter() {
			public long getTotalSize() throws Exception {
				return (Long)getJdbcTemplate().queryForObject(countSql, args, Long.class);
			}
		};
	}
	
	protected void appendSqlToSelectFragmentIdsTaggedWithAnyOf(
		StringBuilder sql, Set<Long> tagIds, boolean sort) {
		
		sql.append("select distinct f.fragment_id");
		if (sort) appendFieldForSort(sql, "f.");
    sql.append(" from fragment as f, tagging as t");
    sql.append(" where f.fragment_id = t.target_id");
    sql.append(" and t.target_type = " + QueryUtils.TAGGING_TARGET_FRAGMENT);
		sql.append(" and t.tag_id in (");
		sql.append(joinToString(tagIds, ", "));
		sql.append(")");
	}
	
	protected void appendFieldForSort(StringBuilder sql, String prefix) {
		if (getSortOption().orderBy.isString()) 
			sql.append(", " + ignoreCaseForSort(getSortOption().orderBy.getName(), prefix));
		else
			sql.append(", " + prefix + getSortOption().orderBy.getName());
	}
}
