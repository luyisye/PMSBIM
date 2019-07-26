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
 * ���Ѹ��Ʊ�����������̽��������ݸ�NC
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
			log.info("���롶�Ѹ��Ʊ�����������̽�����ֵ��NC����...");
			requestid = requestInfo.getRequestid();
			billid = requestInfo.getRequestManager().getBillid() + "";
			Map<String, String> yfkMainMap = TableDataUtil.getMainMap(log, requestInfo);// ����
			// List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log,
			// requestInfo, 0);// ��ϸ��1
			// ����NC-WebService�ӿ�
			String xml = getYfkXml(yfkMainMap);// �ӿڲ���
			log.info("xml:" + xml);
			weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
			weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator
					.getPMWebserviceSOAP11port_http();
			String result = service.voucherOrder(xml, "", "", "", "", "", "", "", "", "", "");
			String key = " successful=\"";// NC�ӿڷ���ֵ��key��[ successful="]
			int index = result.indexOf(key);// key�ڷ���ֵ�ַ����г��ֵĵ�һ�ε��±�
			String successRst = result.substring(index + key.length(), index + key.length() + 1);// ���ص�successful����ֵ��Y-�ɹ���N-ʧ��
			Map<String, String> contentMap = XmlUtil.xmlToMap(result, "UTF-8");
			if (successRst.equals("Y")) {
				log.info("�ӿڵ��óɹ�������ֵΪ��\n" + result);
			} else {
				log.info("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
				requestInfo.getRequestManager().setMessagecontent("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
				return "0";
			}
		} catch (Exception e) {
			log.error(e);
		}

		return SUCCESS;
	}

	/**
	 * ��ȡ���ñ���XML
	 * 
	 * @param yfkMainMap ���Ѹ��Ʊ����������������
	 * @return NC�涨�Ľӿ���������
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 17:00
	 */
	private String getYfkXml(Map<String, String> yfkMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA����NC��һ�����ñ�����'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA���̱��
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// ��˾��֯
		xmlSb.append("<vouchertype>").append("����ƾ֤").append("</vouchertype>");// ƾ֤��𣺹̶�ֵ ����ƾ֤
		xmlSb.append("<dbilldate>").append(yfkMainMap.get("sqrq")).append("</dbilldate>");// ƾ֤����
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// �Ƶ��˱��� ��ʱֻ��ʹ��hlj
		if (yfkMainMap.get("xgfj") != "") {
			int fjsl = yfkMainMap.get("xgfj").split(",").length;// ��������
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// ������
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// ������
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// �跽���ϼ�
		// �跽��Ŀ
		int inid = 1;
		String sql = "select *,zy,jfkm,fzhsbm,fycdbm,ifnull(sum(rzje),0) as rzje from formtable_main_44_dt1 where mainid="
				+ billid + " group by jfkm,fzhsbm,fycdbm";// ���ݽ跽��Ŀ������������Ŀ�����óе����ŷ���
		log.info("sql:" + sql);
		RecordSet rs = new RecordSet();
		rs.execute(sql);
		while (rs.next()) {
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append(rs.getString("jfkm")).append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(rs.getString("rzje")).append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			// String fzhsbm=rs.getString("fzhsbm");//����������Ŀ
			String yfxm = rs.getString("yfxm");// ��Ŀ
			log.info("yf:" + yfxm);
			if (!yfxm.equals("")) // ��Ŀ��Ϊ��
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("�з���Ŀ").append("</pk_Checktype>");// ��Ŀ������������
				xmlSb.append("<pk_Checkvalue>").append(getXMBM(rs.getString("yfxm"))).append("</pk_Checkvalue>");// ��Ŀ�����������
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
				xmlSb.append("</item>");
			} else {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
			sumjfje += Util.getDoubleValue(rs.getString("rzje"));// ���н跽���ϼ�
		}
		// �跽��Ŀ22211601��˰�ʣ�
		if(yfkMainMap.get("rzcyhj")!=""&&yfkMainMap.get("rzcyhj")!="0.00") {
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(yfkMainMap.get("rzcyhj")).append("</debitamount>");// �跽���(���˲���ϼ�)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		sumjfje += Util.getDoubleValue(rs.getString("rzje"));// ���н跽���ϼ�
		inid++;
		// ������Ŀ

		String sql2 = "select *,ifnull(sum(rzje),0)+ifnull(sum(rzcy),0) as dfje from formtable_main_44_dt1 where mainid="
				+ billid + "";// ���ݽ跽��Ŀ������������Ŀ�����óе����ŷ���
		log.info("sql2:" + sql2);
		RecordSet rs2 = new RecordSet();
		RecordSet rs3 = new RecordSet();
		rs2.execute(sql2);
		String sqlmain = "select * from formtable_main_44 where requestid="+requestid+"";
		rs3.execute(sqlmain);
		while (rs2.next()) {
			log.info("inid��" + inid);
			log.info("yfkdbh:" + rs2.getString("yfkdbh"));
			String dfkm = getDFCode(rs2.getString("yfkdbh"));// ������Ŀ
			log.info("dfkm:" + dfkm);
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs2.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append(dfkm).append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append(rs2.getString("dfje")).append("</creditamount>");// �������
			xmlSb.append("<ass>");
			if (dfkm.equals("122105")) // ����Ӧ��-��λ��
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա����������������
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("fzhsry")).append("</pk_Checkvalue>");// ��Ա���������������
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("�ͻ�����").append("</pk_Checktype>");// ������������������
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// �����������������
				xmlSb.append("</item>");
			} else if (dfkm.equals("122103"))// ����Ӧ��-��֤��
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(rs2.getString("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("�ͻ�����").append("</pk_Checktype>");// �ͻ�����������������
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// �ͻ����������������
				xmlSb.append("</item>");
			} else if (dfkm.equals("122102"))// ����Ӧ��-���ý�
			{
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա����������������
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("fzhsry")).append("</pk_Checkvalue>");// ��Ա���������������
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(rs3.getString("dffzhsbm")).append("</pk_Checkvalue>");// ���Ŵ��������������
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
			// sumjfje+=Util.getDoubleValue(rs.getString("rzje"));//���н跽���ϼ�
		}

		// ---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}

	/* ��ȡ�з���Ŀ���� */
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

	/* ͨ���Ѹ���Ż�ȡ������̵Ľ跽��Ŀ���� */
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
	 * ͨ���Ѹ���ź����formid��ȡ������̵Ľ跽��Ŀ��� public String getDFMoney(String dh,int formid)
	 * { String money=""; String
	 * sql="select * from formtable_main_"+(-formid)+" where requestid="+dh+"";
	 * RecordSet rs = new RecordSet(); rs.execute(sql); if(rs.next()) {
	 * money=rs.getString("jfje"); } return money; }
	 */
}
