package weaver.pmsbim.eric.action;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.conn.RecordSet;
import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.workflow.WorkflowComInfo;

/**
 * 《已付款发票核销单》流程结束后传数据给NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 20:00
 */
public class Yfk2NCAction implements Action {
	private Log log = LogFactory.getLog(Yfk2NCAction.class.getName());
	String requestid = "";
	String billid = "";

	@Override
	public String execute(RequestInfo requestInfo) {
		try {
			log.info("进入《已付款发票核销单》流程结束传值给NC方法...");
			requestid = requestInfo.getRequestid();
			billid = requestInfo.getRequestManager().getBillid() + "";
			Map<String, String> yfkMainMap = TableDataUtil.getMainMap(log, requestInfo);// 主表
			// List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log,
			// requestInfo, 0);// 明细表1
			// 调用NC-WebService接口
			String xml = getYfkXml(yfkMainMap);// 接口参数
			log.info("xml:" + xml);
			weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
			weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator
					.getPMWebserviceSOAP11port_http();
			String result = service.voucherOrder(xml, "", "", "", "", "", "", "", "", "", "");
			String key = " successful=\"";// NC接口返回值的key：[ successful="]
			int index = result.indexOf(key);// key在返回值字符串中出现的第一次的下标
			String successRst = result.substring(index + key.length(), index + key.length() + 1);// 返回的successful属性值：Y-成功；N-失败
			Map<String, String> contentMap = XmlUtil.xmlToMap(result, "UTF-8");
			if (successRst.equals("Y")) {
				log.info("接口调用成功！返回值为：\n" + result);
			} else {
				log.info("接口调用失败！返回值为：\n" + result);
				requestInfo.getRequestManager().setMessagecontent("接口调用失败！返回值为：\n" + result);
				return "0";
			}
		} catch (Exception e) {
			log.error(e);
		}

		return SUCCESS;
	}

	/**
	 * 获取费用报销XML
	 * 
	 * @param yfkMainMap 《已付款发票核销单》主表数据
	 * @return NC规定的接口所需数据
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 17:00
	 */
	private String getYfkXml(Map<String, String> yfkMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA传送NC的一个费用报销单'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA流程编号
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// 公司组织
		xmlSb.append("<vouchertype>").append("记账凭证").append("</vouchertype>");// 凭证类别：固定值 记账凭证
		xmlSb.append("<dbilldate>").append(yfkMainMap.get("sqrq")).append("</dbilldate>");// 凭证日期
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// 制单人编码 暂时只能使用hlj
		if (yfkMainMap.get("xgfj") != "") {
			int fjsl = yfkMainMap.get("xgfj").split(",").length;// 附件数量
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// 附件数
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// 附件数
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// 借方金额合计
		// 借方科目
		int inid = 1;
		String sql = "select *,zy,jfkm,fzhsbm,fycdbm,ifnull(sum(rzje),0) as rzje from formtable_main_44_dt1 where mainid="
				+ billid + " group by jfkm,fzhsbm,fycdbm";// 根据借方科目，辅助核算项目，费用承担部门分组
		log.info("sql:" + sql);
		RecordSet rs = new RecordSet();
		rs.execute(sql);
		while (rs.next()) {
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append(rs.getString("jfkm")).append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(rs.getString("rzje")).append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			// String fzhsbm=rs.getString("fzhsbm");//辅助核算项目
			String yfxm = rs.getString("yfxm");// 项目
			log.info("yf:" + yfxm);
			if (!yfxm.equals("")) // 项目不为空
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("研发项目").append("</pk_Checktype>");// 项目辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(getXMBM(rs.getString("yfxm"))).append("</pk_Checkvalue>");// 项目辅助核算编码
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
				xmlSb.append("</item>");
			} else {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
			sumjfje += Util.getDoubleValue(rs.getString("rzje"));// 所有借方金额合计
		}
		// 借方科目22211601（税率）
		if(yfkMainMap.get("rzcyhj")!=""&&yfkMainMap.get("rzcyhj")!="0.00") {
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(yfkMainMap.get("rzcyhj")).append("</debitamount>");// 借方金额(入账差异合计)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		sumjfje += Util.getDoubleValue(rs.getString("rzje"));// 所有借方金额合计
		inid++;
		// 贷方科目

		String sql2 = "select *,ifnull(sum(rzje),0)+ifnull(sum(rzcy),0) as dfje from formtable_main_44_dt1 where mainid="
				+ billid + "";// 根据借方科目，辅助核算项目，费用承担部门分组
		log.info("sql2:" + sql2);
		RecordSet rs2 = new RecordSet();
		RecordSet rs3 = new RecordSet();
		rs2.execute(sql2);
		String sqlmain = "select * from formtable_main_44 where requestid="+requestid+"";
		rs3.execute(sqlmain);
		while (rs2.next()) {
			log.info("inid：" + inid);
			log.info("yfkdbh:" + rs2.getString("yfkdbh"));
			String dfkm = getDFCode(rs2.getString("yfkdbh"));// 贷方科目
			log.info("dfkm:" + dfkm);
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs2.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append(dfkm).append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append(rs2.getString("dfje")).append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			if (dfkm.equals("122105")) // 其他应收-单位款
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员档案辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("fzhsry")).append("</pk_Checkvalue>");// 人员档案辅助核算编码
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("客户档案").append("</pk_Checktype>");// 户档案辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// 户档案辅助核算编码
				xmlSb.append("</item>");
			} else if (dfkm.equals("122103"))// 其他应收-保证金
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs2.getString("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("客户档案").append("</pk_Checktype>");// 客户档案辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// 客户档案辅助核算编码
				xmlSb.append("</item>");
			} else if (dfkm.equals("122102"))// 其他应收-备用金
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员档案辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("fzhsry")).append("</pk_Checkvalue>");// 人员档案辅助核算编码
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("dffzhsbm")).append("</pk_Checkvalue>");// 部门贷方辅助核算编码
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
			// sumjfje+=Util.getDoubleValue(rs.getString("rzje"));//所有借方金额合计
		}

		// ---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}

	/* 获取研发项目编码 */
	public String getXMBM(String id) {
		String bm = "";
		RecordSet rs = new RecordSet();
		String sql = "select * from uf_yfxm where id=" + id;
		rs.execute(sql);
		if (rs.next()) {
			bm = rs.getString("yfxmbm");
		}
		return bm;
	}

	/* 通过已付款单号获取相关流程的借方科目编码 */
	public String getDFCode(String dh) {
		String code = "";
		String workflowid = "";
		int formid = 0;
		String sql = "select * from workflow_requestbase where requestid=" + dh + "";
		log.info("sql:" + sql);
		RecordSet rs = new RecordSet();
		rs.execute(sql);
		if (rs.next()) {
			workflowid = rs.getString("workflowid");
		}
		WorkflowComInfo workflowComInfo = null;
		try {
			workflowComInfo = new WorkflowComInfo();
			workflowComInfo.reloadWorkflowInfos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String formIds = workflowComInfo.getFormId(workflowid);
		formid = Util.getIntValue(formIds, 0);
		String sql2 = "select * from  formtable_main_" + (-formid) + " where requestid=" + dh + "";
		RecordSet rs2 = new RecordSet();
		rs2.execute(sql2);
		if (rs2.next()) {
			code = rs2.getString("jfkm");
		}
		log.info("code:" + code);
		return code;
	}

	/*
	 * 通过已付款单号和浏览formid获取相关流程的借方科目金额 public String getDFMoney(String dh,int formid)
	 * { String money=""; String
	 * sql="select * from formtable_main_"+(-formid)+" where requestid="+dh+"";
	 * RecordSet rs = new RecordSet(); rs.execute(sql); if(rs.next()) {
	 * money=rs.getString("jfje"); } return money; }
	 */
}
