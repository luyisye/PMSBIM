package weaver.pmsbim.eric.action;

import java.util.List;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.conn.RecordSet;
import weaver.conn.RecordSetDataSource;
import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;

/**
 * 《差旅费费用报销》流程结束后传数据给NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 14:00
 */
public class Clfbx2NCAction implements Action {
	private Log log = LogFactory.getLog(Clfbx2NCAction.class.getName());
	String requestid = "";
	List<Map<String, String>> listMap2 = null;

	@Override
	public String execute(RequestInfo requestInfo) {
		try {
			requestid = requestInfo.getRequestid();
			log.info("进入《差旅费报销》流程结束传值给NC方法...");
			Map<String, String> clfMainMap = TableDataUtil.getMainMap(log, requestInfo);// 主表
			listMap2 = TableDataUtil.getDtListMap(log, requestInfo, 0);// 明细表1

			// 调用NC-WebService接口
			String xml = getClfXml(clfMainMap);// 接口参数
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
	 * @param clfMainMap 《差旅费费用报销》主表数据
	 * @param listMap1   明细表3
	 * @return NC规定的接口所需数据
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 14:00
	 */
	private String getClfXml(Map<String, String> clfMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		int inid=1;
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA传送NC的一个费用报销单'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA流程编号
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// 公司组织
		xmlSb.append("<vouchertype>").append("记账凭证").append("</vouchertype>");// 凭证类别：固定值 记账凭证
		xmlSb.append("<dbilldate>").append(clfMainMap.get("sqrq")).append("</dbilldate>");// 凭证日期
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// 制单人编码 暂时只能使用hlj
		if (clfMainMap.get("xgfj") != "") {
			int fjsl = clfMainMap.get("xgfj").split(",").length;// 附件数量
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// 附件数
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// 附件数
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// 借方金额合计
		//String bxdjString = clfMainMap.get("bxdj");//报销调碱
		//String rzjeString = clfMainMap.get("rzje");//入账金额
		//double je = Util.getDoubleValue(clfMainMap.get("rzje"))-Util.getDoubleValue(clfMainMap.get("bxdj"));//入账差异-报销调减
		//借方科目(差旅费)传凭证
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
		xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// 摘要
		xmlSb.append("<accsubjcode>").append(clfMainMap.get("jfkm")).append("</accsubjcode>");// 科目编码
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
		xmlSb.append("<debitamount>").append(clfMainMap.get("rzje")).append("</debitamount>");// 借方金额
		xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
		xmlSb.append("<ass>");
		if (!clfMainMap.get("fzhsbm").equals("")) {
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
			xmlSb.append("</item>");
		}
		log.info("yfxm:"+clfMainMap.get("yfxm"));
		if (!clfMainMap.get("yfxm").equals("")) {
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("研发项目").append("</pk_Checktype>");// 研发项目辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(getXMBM(clfMainMap.get("yfxm"))).append("</pk_Checkvalue>");// 研发项目辅助核算编码
			xmlSb.append("</item>");
		}
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		inid++;
		//借方科目(进项税22211601待认证进项税-系统认证)传凭证
		if(Util.getDoubleValue(clfMainMap.get("rzcy"),0.00)!=0.00){//入账差异不为空
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// 待认证进项税-系统认证科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(clfMainMap.get("rzcy")).append("</debitamount>");// 借方金额(入账差异1)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//借方科目(进项税22211602待认证进项税-其他抵扣认证)传凭证
		log.info("入账差异2："+clfMainMap.get("rzcy2"));
		if(Util.getDoubleValue(clfMainMap.get("rzcy2"),0.00)!=0.00) {//入账差异2不为空
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("22211602").append("</accsubjcode>");// 待认证进项税-其他抵扣认证科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(clfMainMap.get("rzcy2")).append("</debitamount>");// 借方金额(入账差异2)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//所有借方金额合计
		sumjfje=Util.getDoubleValue(clfMainMap.get("rzcy"))+Util.getDoubleValue(clfMainMap.get("rzcy2"))+Util.getDoubleValue(clfMainMap.get("rzje"));
		// 贷方科目传凭证（打款金额+冲抵金额）
		boolean flag=false;
		if(clfMainMap.get("dkje")!="") {//如果打款金额不为空
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(clfMainMap.get("dkje"))).append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("银行账户").append("</pk_Checktype>");// 辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// 辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			flag=true;
		}
		if(clfMainMap.get("cdje")!="") {//如果冲抵金额不为空
			xmlSb.append("<detail>");
			if(flag) {
				xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// 行号
			}else {
				xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			}		
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("122102").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(clfMainMap.get("cdje"))).append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员档案辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsry")).append("</pk_Checkvalue>");// 人员档案辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		//---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}
	/* 获取研发项目编码 */
	public String getXMBM(String id) {
		String bm="";
		RecordSet rs = new RecordSet();
		String sql="select * from uf_yfxm where id="+id;
		rs.execute(sql);
		if(rs.next()) {
			bm=rs.getString("yfxmbm");
		}
		return bm;
	}



}
