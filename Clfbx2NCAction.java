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
 * �����÷ѷ��ñ��������̽��������ݸ�NC
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
			log.info("���롶���÷ѱ��������̽�����ֵ��NC����...");
			Map<String, String> clfMainMap = TableDataUtil.getMainMap(log, requestInfo);// ����
			listMap2 = TableDataUtil.getDtListMap(log, requestInfo, 0);// ��ϸ��1

			// ����NC-WebService�ӿ�
			String xml = getClfXml(clfMainMap);// �ӿڲ���
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
	 * @param clfMainMap �����÷ѷ��ñ�������������
	 * @param listMap1   ��ϸ��3
	 * @return NC�涨�Ľӿ���������
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 14:00
	 */
	private String getClfXml(Map<String, String> clfMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		int inid=1;
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA����NC��һ�����ñ�����'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA���̱��
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// ��˾��֯
		xmlSb.append("<vouchertype>").append("����ƾ֤").append("</vouchertype>");// ƾ֤��𣺹̶�ֵ ����ƾ֤
		xmlSb.append("<dbilldate>").append(clfMainMap.get("sqrq")).append("</dbilldate>");// ƾ֤����
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// �Ƶ��˱��� ��ʱֻ��ʹ��hlj
		if (clfMainMap.get("xgfj") != "") {
			int fjsl = clfMainMap.get("xgfj").split(",").length;// ��������
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// ������
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// ������
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// �跽���ϼ�
		//String bxdjString = clfMainMap.get("bxdj");//��������
		//String rzjeString = clfMainMap.get("rzje");//���˽��
		//double je = Util.getDoubleValue(clfMainMap.get("rzje"))-Util.getDoubleValue(clfMainMap.get("bxdj"));//���˲���-��������
		//�跽��Ŀ(���÷�)��ƾ֤
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
		xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// ժҪ
		xmlSb.append("<accsubjcode>").append(clfMainMap.get("jfkm")).append("</accsubjcode>");// ��Ŀ����
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
		xmlSb.append("<debitamount>").append(clfMainMap.get("rzje")).append("</debitamount>");// �跽���
		xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
		xmlSb.append("<ass>");
		if (!clfMainMap.get("fzhsbm").equals("")) {
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
			xmlSb.append("</item>");
		}
		log.info("yfxm:"+clfMainMap.get("yfxm"));
		if (!clfMainMap.get("yfxm").equals("")) {
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("�з���Ŀ").append("</pk_Checktype>");// �з���Ŀ������������
			xmlSb.append("<pk_Checkvalue>").append(getXMBM(clfMainMap.get("yfxm"))).append("</pk_Checkvalue>");// �з���Ŀ�����������
			xmlSb.append("</item>");
		}
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		inid++;
		//�跽��Ŀ(����˰22211601����֤����˰-ϵͳ��֤)��ƾ֤
		if(Util.getDoubleValue(clfMainMap.get("rzcy"),0.00)!=0.00){//���˲��첻Ϊ��
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// ����֤����˰-ϵͳ��֤��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(clfMainMap.get("rzcy")).append("</debitamount>");// �跽���(���˲���1)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//�跽��Ŀ(����˰22211602����֤����˰-�����ֿ���֤)��ƾ֤
		log.info("���˲���2��"+clfMainMap.get("rzcy2"));
		if(Util.getDoubleValue(clfMainMap.get("rzcy2"),0.00)!=0.00) {//���˲���2��Ϊ��
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("22211602").append("</accsubjcode>");// ����֤����˰-�����ֿ���֤��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(clfMainMap.get("rzcy2")).append("</debitamount>");// �跽���(���˲���2)
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//���н跽���ϼ�
		sumjfje=Util.getDoubleValue(clfMainMap.get("rzcy"))+Util.getDoubleValue(clfMainMap.get("rzcy2"))+Util.getDoubleValue(clfMainMap.get("rzje"));
		// ������Ŀ��ƾ֤�������+��ֽ�
		boolean flag=false;
		if(clfMainMap.get("dkje")!="") {//�������Ϊ��
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(clfMainMap.get("dkje"))).append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("�����˻�").append("</pk_Checktype>");// ������������
			xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// �����������
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			flag=true;
		}
		if(clfMainMap.get("cdje")!="") {//�����ֽ�Ϊ��
			xmlSb.append("<detail>");
			if(flag) {
				xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// �к�
			}else {
				xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			}		
			xmlSb.append("<explanation>").append(clfMainMap.get("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("122102").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(clfMainMap.get("cdje"))).append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա����������������
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsry")).append("</pk_Checkvalue>");// ��Ա���������������
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
			xmlSb.append("<pk_Checkvalue>").append(clfMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
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
	/* ��ȡ�з���Ŀ���� */
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
