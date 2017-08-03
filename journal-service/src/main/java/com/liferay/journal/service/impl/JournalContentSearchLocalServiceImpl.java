/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.journal.service.impl;

import com.liferay.journal.model.JournalContentSearch;
import com.liferay.journal.service.base.JournalContentSearchLocalServiceBaseImpl;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.dao.orm.custom.sql.CustomSQLUtil;
import com.liferay.portal.kernel.dao.orm.WildcardMode;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.PortletPreferences;
import com.liferay.portal.kernel.portlet.DisplayInformationProvider;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.util.PortletKeys;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Brian Wing Shun Chan
 * @author Wesley Gong
 */
public class JournalContentSearchLocalServiceImpl
	extends JournalContentSearchLocalServiceBaseImpl {

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		Bundle bundle = FrameworkUtil.getBundle(
			JournalContentSearchLocalServiceImpl.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceTrackerMap = ServiceTrackerMapFactory.singleValueMap(
			bundleContext, DisplayInformationProvider.class,
			"javax.portlet.name");

		_serviceTrackerMap.open();
	}

	@Override
	public void checkContentSearches(long companyId) throws PortalException {
		if (_log.isInfoEnabled()) {
			_log.info("Checking journal content search for " + companyId);
		}

		journalContentSearchPersistence.removeByCompanyId(companyId);

		for (String portletId : _serviceTrackerMap.keySet()) {
			DisplayInformationProvider displayInformationProvider =
				_serviceTrackerMap.getService(portletId);

			List<PortletPreferences> portletPreferencesList =
				portletPreferencesLocalService.getPortletPreferences(
					companyId, PortletKeys.PREFS_OWNER_ID_DEFAULT,
					PortletKeys.PREFS_OWNER_TYPE_LAYOUT,
					CustomSQLUtil.keywords(
						portletId.concat(_INSTANCE_SEPARATOR),
						WildcardMode.TRAILING)[0]);

			for (PortletPreferences portletPreferences :
					portletPreferencesList) {

				javax.portlet.PortletPreferences portletPreferencesImpl =
					PortletPreferencesFactoryUtil.fromDefaultXML(
						portletPreferences.getPreferences());

				String classPK = displayInformationProvider.getClassPK(
					portletPreferencesImpl);

				Layout layout = layoutLocalService.getLayout(
					portletPreferences.getPlid());

				_addContentSearch(
					layout.getGroupId(), companyId, layout.isPrivateLayout(),
					layout.getLayoutId(), portletPreferences.getPortletId(),
					classPK);
			}
		}
	}

	@Override
	public void deleteArticleContentSearch(
		long groupId, boolean privateLayout, long layoutId, String portletId) {

		journalContentSearchPersistence.removeByG_P_L_P(
			groupId, privateLayout, layoutId, portletId);
	}

	@Override
	public void deleteArticleContentSearch(
		long groupId, boolean privateLayout, long layoutId, String portletId,
		String articleId) {

		JournalContentSearch contentSearch =
			journalContentSearchPersistence.fetchByG_P_L_P_A(
				groupId, privateLayout, layoutId, portletId, articleId);

		if (contentSearch != null) {
			deleteJournalContentSearch(contentSearch);
		}
	}

	@Override
	public void deleteArticleContentSearches(long groupId, String articleId) {
		List<JournalContentSearch> contentSearches =
			journalContentSearchPersistence.findByG_A(groupId, articleId);

		for (JournalContentSearch contentSearch : contentSearches) {
			deleteJournalContentSearch(contentSearch);
		}
	}

	@Override
	public void deleteLayoutContentSearches(
		long groupId, boolean privateLayout, long layoutId) {

		List<JournalContentSearch> contentSearches =
			journalContentSearchPersistence.findByG_P_L(
				groupId, privateLayout, layoutId);

		for (JournalContentSearch contentSearch : contentSearches) {
			deleteJournalContentSearch(contentSearch);
		}
	}

	@Override
	public void deleteOwnerContentSearches(
		long groupId, boolean privateLayout) {

		List<JournalContentSearch> contentSearches =
			journalContentSearchPersistence.findByG_P(groupId, privateLayout);

		for (JournalContentSearch contentSearch : contentSearches) {
			deleteJournalContentSearch(contentSearch);
		}
	}

	@Override
	public void destroy() {
		super.destroy();

		_serviceTrackerMap.close();
	}

	@Override
	public List<JournalContentSearch> getArticleContentSearches() {
		return journalContentSearchPersistence.findAll();
	}

	@Override
	public List<JournalContentSearch> getArticleContentSearches(
		long groupId, String articleId) {

		return journalContentSearchPersistence.findByG_A(groupId, articleId);
	}

	@Override
	public List<JournalContentSearch> getArticleContentSearches(
		String articleId) {

		return journalContentSearchPersistence.findByArticleId(articleId);
	}

	@Override
	public List<Long> getLayoutIds(
		long groupId, boolean privateLayout, String articleId) {

		List<Long> layoutIds = new ArrayList<>();

		List<JournalContentSearch> contentSearches =
			journalContentSearchPersistence.findByG_P_A(
				groupId, privateLayout, articleId);

		for (JournalContentSearch contentSearch : contentSearches) {
			layoutIds.add(contentSearch.getLayoutId());
		}

		return layoutIds;
	}

	@Override
	public int getLayoutIdsCount(
		long groupId, boolean privateLayout, String articleId) {

		return journalContentSearchPersistence.countByG_P_A(
			groupId, privateLayout, articleId);
	}

	@Override
	public int getLayoutIdsCount(String articleId) {
		return journalContentSearchPersistence.countByArticleId(articleId);
	}

	@Override
	public List<JournalContentSearch> getPortletContentSearches(
		String portletId) {

		return journalContentSearchPersistence.findByPortletId(portletId);
	}

	@Override
	public JournalContentSearch updateContentSearch(
			long groupId, boolean privateLayout, long layoutId,
			String portletId, String articleId)
		throws PortalException {

		return updateContentSearch(
			groupId, privateLayout, layoutId, portletId, articleId, false);
	}

	@Override
	public JournalContentSearch updateContentSearch(
			long groupId, boolean privateLayout, long layoutId,
			String portletId, String articleId, boolean purge)
		throws PortalException {

		JournalContentSearch contentSearch = null;

		if (purge) {
			journalContentSearchPersistence.removeByG_P_L_P(
				groupId, privateLayout, layoutId, portletId);
		}
		else {
			contentSearch = journalContentSearchPersistence.fetchByG_P_L_P_A(
				groupId, privateLayout, layoutId, portletId, articleId);
		}

		if (contentSearch == null) {
			Group group = groupLocalService.getGroup(groupId);

			contentSearch = _addContentSearch(
				groupId, group.getCompanyId(), privateLayout, layoutId,
				portletId, articleId);
		}

		return contentSearch;
	}

	@Override
	public List<JournalContentSearch> updateContentSearch(
			long groupId, boolean privateLayout, long layoutId,
			String portletId, String[] articleIds)
		throws PortalException {

		journalContentSearchPersistence.removeByG_P_L_P(
			groupId, privateLayout, layoutId, portletId);

		List<JournalContentSearch> contentSearches = new ArrayList<>();

		for (String articleId : articleIds) {
			JournalContentSearch contentSearch = updateContentSearch(
				groupId, privateLayout, layoutId, portletId, articleId, false);

			contentSearches.add(contentSearch);
		}

		return contentSearches;
	}

	private JournalContentSearch _addContentSearch(
		long groupId, long companyId, boolean privateLayout, long layoutId,
		String portletId, String articleId) {

		long contentSearchId = counterLocalService.increment();

		JournalContentSearch contentSearch =
			journalContentSearchPersistence.create(contentSearchId);

		contentSearch.setGroupId(groupId);
		contentSearch.setCompanyId(companyId);
		contentSearch.setPrivateLayout(privateLayout);
		contentSearch.setLayoutId(layoutId);
		contentSearch.setPortletId(portletId);
		contentSearch.setArticleId(articleId);

		return journalContentSearchPersistence.update(contentSearch);
	}

	private static final String _INSTANCE_SEPARATOR = "_INSTANCE_";

	private static final Log _log = LogFactoryUtil.getLog(
		JournalContentSearchLocalServiceImpl.class);

	private ServiceTrackerMap<String, DisplayInformationProvider>
		_serviceTrackerMap;

}