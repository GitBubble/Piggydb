package marubinotto.piggydb.ui.page;

import java.util.List;

import marubinotto.piggydb.model.Filter;
import marubinotto.piggydb.model.RelatedTags;
import marubinotto.piggydb.model.RelatedTags.RelatedTag;
import marubinotto.piggydb.model.query.FragmentsByFilter;
import marubinotto.piggydb.model.Tag;
import marubinotto.piggydb.ui.page.common.AbstractFragmentsPage;
import marubinotto.piggydb.ui.page.common.PageUrl;
import marubinotto.piggydb.ui.page.control.TagTree;
import marubinotto.piggydb.ui.page.control.form.SingleTagForm;
import marubinotto.piggydb.ui.page.model.RecentlyViewed;
import marubinotto.util.Assert;
import marubinotto.util.message.CodedException;
import marubinotto.util.procedure.Procedure;
import net.sf.click.control.ActionLink;
import net.sf.click.control.Form;
import net.sf.click.control.HiddenField;
import net.sf.click.control.Submit;
import net.sf.click.control.TextField;
import net.sf.click.extras.tree.Tree;

import org.apache.commons.lang.StringUtils;

public class FilterPage extends AbstractFragmentsPage {

	@Override
	protected PageUrl createThisPageUrl() {
		PageUrl pageUrl = super.createThisPageUrl();
		if (this.filter != null && this.filter.getId() != null) {
			pageUrl.parameters.put(PN_FILTER_ID, this.filter.getId());
		}
		return pageUrl;
	}

	@Override
	protected String getAtomUrl() {
		if (this.filter.getId() == null) return null;
		return getDefaultAtomUrl() + "?id=" + this.filter.getId();
	}

	//
	// Input
	//

	public static final String PN_FILTER_ID = "id";
	public static final String PN_NEW = "new";
	public static final String PN_TAG_IDS = "tagIds";
	public static final String PN_EXCLUDE_TAG_IDS = "exTagIds";

	public Long id;

	public Filter filter;

	public static final String SK_NEW_FILTER = FilterPage.class.getName() + "#filter";
	// Filter attribute(Entity.getAttributes): a dirty flag for a new filter
	public static final String MA_NEW_INSTANCE_MODIFIED = "newInstanceModified";

	public Filter getFilter() {
		return this.filter;
	}

	private boolean isFilterNewInstanceAndModified() {
		Boolean flag = (Boolean)this.filter.getAttributes().get(MA_NEW_INSTANCE_MODIFIED);
		return flag != null ? flag : false;
	}

	@Override
	protected boolean onPreInit() throws Exception {
		if (StringUtils.isNotBlank(getContext().getRequestParameter(PN_NEW)))
			getContext().removeSessionAttribute(SK_NEW_FILTER);

		setTargetFilter();
		if (this.id != null && this.filter == null && !getContext().isAjaxRequest()) {
			setRedirectWithMessage(FilterPage.class, getMessage("FilterPage-no-such-filter", this.id));
			return false;
		}

		if (updateFilterWithParameters()) {
			setRedirect(createThisPageUrl().getPagePath());
			return false;
		}

		return true;
	}

	private void setTargetFilter() throws Exception {
		if (this.id != null) {
			this.filter = getDomain().getFilterRepository().get(this.id);
		}
		else {
			this.filter = (Filter)getContext().getSessionAttribute(SK_NEW_FILTER);
			if (this.filter == null) {
				this.filter = createNewFilter();
				getContext().setSessionAttribute(SK_NEW_FILTER, this.filter);
			}
		}
	}

	private Filter createNewFilter() throws Exception {
		Filter filter = getDomain().getFilterRepository().newInstance(getUser());
		Tag trashTag = getDomain().getTagRepository().getTrashTag();
		if (trashTag != null) filter.addExcludeByUser(trashTag, getUser());
		return filter;
	}

	private boolean updateFilterWithParameters() throws Exception {
		String[] tagIds = getContext().getRequestParameterValues(PN_TAG_IDS);
		String[] exTagIds = getContext().getRequestParameterValues(PN_EXCLUDE_TAG_IDS);
		if (tagIds != null || exTagIds != null) {
			if (tagIds != null) {
				for (String tagId : tagIds) {
					Tag tag = getDomain().getTagRepository().get(Long.parseLong(tagId));
					if (tag != null) this.filter.addIncludeByUser(tag, getUser());
				}
			}
			if (exTagIds != null) {
				for (String tagId : exTagIds) {
					Tag tag = getDomain().getTagRepository().get(Long.parseLong(tagId));
					if (tag != null) this.filter.addExcludeByUser(tag, getUser());
				}
			}
			onFilterChange();
			return true;
		}
		return false;
	}

	//
	// Control
	//

	@Override
	public void onInit() {
		super.onInit();
		
		initControls();
		if (this.filter != null) applyTargetFilterToControls();
	}

	private void initControls() {
		// Filter Name
		this.filterNameField = new TextField("filterName");
		this.filterNameField.setRequired(true);
		this.filterNameField.setSize(20);
		this.filterNameField.setMaxLength(50);
		this.filterNameForm.add(this.filterNameField);
		this.filterNameForm.add(new Submit("saveFilter",
			getMessage("FilterPage-save"), this, "onSaveFilterClick"));

		// Includes
		this.includeTagForm.setListenerForAdd("onAddIncludeTagClick");
		this.includeTagForm.setListenerForDelete("onDeleteIncludeTagClick");
		this.includeTagForm.initialize();
		this.includeTags = new TagTree("includeTags", this.resources, this.html);
		addControl(this.includeTags);

		// Excludes
		this.excludeTagForm.setListenerForAdd("onAddExcludeTagClick");
		this.excludeTagForm.setListenerForDelete("onDeleteExcludeTagClick");
		this.excludeTagForm.initialize();
		this.excludeTags = new TagTree("excludeTags", this.resources, this.html);
		addControl(this.excludeTags);
	}

	private void applyTargetFilterToControls() {
		Assert.Property.requireNotNull(filter, "filter");

		if (this.filter.getName() != null) {
			this.filterNameField.setValue(this.filter.getName());
		}

		boolean canChange = this.filter.canChange(getUser());
		TagTree.restoreTagTree(this.includeTags, this.filter.getIncludes(), canChange);
		TagTree.restoreTagTree(this.excludeTags, this.filter.getExcludes(), canChange);
	}

	// Filter Name

	public boolean renameMode = false;
	public ActionLink renameLink = new ActionLink(this, "onRenameClick");
	public ActionLink deleteLink = new ActionLink(this, "onDeleteClick");

	public Form filterNameForm = new Form();
	private TextField filterNameField;

	public boolean onRenameClick() throws Exception {
		this.renameMode = true;
		return true;
	}

	public boolean onDeleteClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		getDomain().getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				getDomain().getFilterRepository().delete(getFilter().getId(), getUser());
				return null;
			}
		});

		getContext().removeSessionAttribute(SK_NEW_FILTER);

		setRedirectWithMessage(HomePage.class,
			getMessage("FilterPage-completed-delete-filter", this.filter.getName()));
		return false;
	}

	public boolean onSaveFilterClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		if (!this.filterNameForm.isValid()) {
			return true;
		}

		if (getUser().isViewer()) {
			this.filterNameForm.setError(getMessage("no-auth-to-save-filter"));
			return true;
		}

		boolean update = (this.filter.getId() != null);
		String filterName = this.filterNameField.getValue();
		String oldName = this.filter.getName();
		this.filter.setNameByUser(filterName, getUser());
		try {
			getLogger().info("Saving a new filter: " + this.filter.getName());
			getDomain().getTransaction().execute(new Procedure() {
				public Object execute(Object input) throws Exception {
					if (getFilter().getId() != null)
						getDomain().getFilterRepository().update(getFilter());
					else
						getDomain().getFilterRepository().register(getFilter());
					return null;
				}
			});
		}
		catch (Exception e) {
			this.filterNameForm.setError(CodedException.getCodedMessageOrThrow(e, this));
			if (oldName != null) this.filter.setNameByUser(oldName, getUser());
			return true;
		}

		this.filter.getAttributes().remove(MA_NEW_INSTANCE_MODIFIED);

		if (update) {
			setRedirectToThisPage(); // No messages for rename
		}
		else {
			setRedirectToThisPage(getMessage("FilterPage-filter-saved", this.filter.getName()));
		}
		return false;
	}

	// Includes

	public SingleTagForm includeTagForm = new SingleTagForm(this);
	private Tree includeTags;

	public boolean onAddIncludeTagClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		String tagName = this.includeTagForm.tagField.getValue();
		if (StringUtils.isBlank(tagName)) {
			return true;
		}

		getLogger().info("Adding an include-tag: " + tagName + " to: " + this.filter.getId());
		Tag tag = getDomain().getTagRepository().getByName(tagName);
		if (tag == null) {
			this.includeTagForm.setError(getMessage("no-such-tag", tagName));
			return true;
		}
		this.filter.addIncludeByUser(tag, getUser());

		onFilterChange();

		setRedirectToThisPage();
		return false;
	}

	public boolean onDeleteIncludeTagClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		String tagToDelete = this.includeTagForm.tagToDeleteField.getValue();
		if (StringUtils.isBlank(tagToDelete)) {
			return true;
		}

		getLogger().info("Deleting an include-tag: " + tagToDelete + " from: " + this.filter.getId());
		this.filter.removeIncludeByUser(tagToDelete, getUser());

		onFilterChange();

		setRedirectToThisPage();
		return false;
	}

	// Excludes

	public SingleTagForm excludeTagForm = new SingleTagForm(this);
	private Tree excludeTags;

	public boolean onAddExcludeTagClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		String tagName = this.excludeTagForm.tagField.getValue();
		if (StringUtils.isBlank(tagName)) {
			return true;
		}

		getLogger().info("Adding a exclude-tag: " + tagName + " to: " + this.filter.getId());
		Tag tag = getDomain().getTagRepository().getByName(tagName);
		if (tag == null) {
			this.excludeTagForm.setError(getMessage("no-such-tag", tagName));
			return true;
		}
		this.filter.addExcludeByUser(tag, getUser());

		onFilterChange();

		setRedirectToThisPage();
		return false;
	}

	public boolean onDeleteExcludeTagClick() throws Exception {
		Assert.Property.requireNotNull(filter, "filter");

		String tagToDelete = this.excludeTagForm.tagToDeleteField.getValue();
		if (StringUtils.isBlank(tagToDelete)) {
			return true;
		}

		getLogger().info("Deleting a exclude-tag: " + tagToDelete + " from: " + this.filter.getId());
		this.filter.removeExcludeByUser(tagToDelete, getUser());

		onFilterChange();

		setRedirectToThisPage();
		return false;
	}

	private void onFilterChange() throws Exception {
		if (this.filter.getId() == null) {
			this.filter.getAttributes().put(MA_NEW_INSTANCE_MODIFIED, true);
			getLogger().debug("The new filter has been modified.");
			return;
		}

		getLogger().info("Saving the filter [" + this.filter.getId() + "] as " + this.filter.getName());
		getDomain().getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				getDomain().getFilterRepository().update(getFilter());
				return null;
			}
		});
	}

	//
	// Model
	//

	public List<RelatedTag> relatedTags;
	public boolean showFilterNameForm = false;

	@Override
	protected void setModels() throws Exception {
		super.setModels();

		Assert.Property.requireNotNull(filter, "filter");

		if (StringUtils.isNotBlank(this.filter.getName())) {
			this.htmlTitle = this.htmlTitle + HTML_TITLE_SEP + this.filter.getName();
		}

		importCss("style/piggydb-filter.css", true, null);

		setRelatedTags();

		if (this.filter.getId() != null) {
			getRecentlyViewed().add(
				new RecentlyViewed.Entity(RecentlyViewed.TYPE_FILTER, this.filter
					.getId()));
		}

		setCommonSidebarModels();

		this.showFilterNameForm = !getUser().isViewer()
			&& (isFilterNewInstanceAndModified() || this.renameMode || !this.filterNameForm.isValid());
	}

	private void setRelatedTags() throws Exception {
		if (this.filter.getIncludes().isEmpty()) return;

		FragmentsByFilter query = (FragmentsByFilter)
			getDomain().getFragmentRepository().getQuery(FragmentsByFilter.class);
		query.setFilter(this.filter);
		RelatedTags relatedTags = query.getRelatedTags();
		this.relatedTags = relatedTags.orderByCount(getDomain().getTagRepository());
	}

	@Override
	public void onRender() {
		super.onRender();
		embedCurrentStateInParameters();
	}

	@SuppressWarnings("unchecked")
	private void embedCurrentStateInParameters() {
		Assert.Property.requireNotNull(filter, "filter");

		Long filterId = this.filter.getId();
		if (filterId != null) {
			this.filterNameForm.add(new HiddenField(PN_FILTER_ID, filterId));
			this.includeTagForm.add(new HiddenField(PN_FILTER_ID, filterId));
			this.excludeTagForm.add(new HiddenField(PN_FILTER_ID, filterId));
			addParameterToCommonForms(PN_FILTER_ID, filterId);
			this.renameLink.getParameters().put(PN_FILTER_ID, filterId);
			this.deleteLink.getParameters().put(PN_FILTER_ID, filterId);
		}
	}
}
