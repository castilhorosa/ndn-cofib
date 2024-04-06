/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>
#define NORMAL 0
#define RECIRC 4
#define CPU    1

const bit<16> TYPE_IPV4      = 0x800;
const bit<16> TYPE_FANTNET   = 0xFFFF;    //reserved RFC1701

const bit<3>  NOT_AT_INGRESS = 1;
const bit<3>  INGRESS_MISS   = 2;
const bit<3>  INGRESS_HIT    = 3;

const bit<3>  NOT_AT_EGRESS  = 7;
const bit<3>  EGRESS_MISS    = 6;
const bit<3>  EGRESS_HIT     = 7;


/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/

typedef bit<9>  egressSpec_t;
typedef bit<48> macAddr_t;
typedef bit<32> ip4Addr_t;


//My defined headers
header num_comp_t {
    bit<8> n;
}

//This header stores the number of pipeline passes
header num_passes_t {
    bit<8> p;
}

header tuple_t {
    bit<8>  table_id;  //table id (1, 2, ..., 31)
    bit<32> crc32;     //Name component hash
    bit<32> Fx;        //Recurrence relation
}

/*
header sw_id_t {
    bit<16>    marker;    //it will be a 16-bit zeros to indicate to the core switches that the packet pass through a CoFIB
    bit<8>     SwId;      //the sw id
}
*/

header ethernet_t {
    macAddr_t dstAddr;
    macAddr_t srcAddr;
    bit<16>   etherType;
}

header ipv4_t {
    bit<4>    version;
    bit<4>    ihl;
    bit<8>    diffserv;
    bit<16>   totalLen;
    bit<16>   identification;
    bit<3>    flags;
    bit<13>   fragOffset;
    bit<8>    ttl;
    bit<8>    protocol;
    bit<16>   hdrChecksum;
    ip4Addr_t srcAddr;
    ip4Addr_t dstAddr;
}

struct metadata {
    @field_list(1)
    bit<8>  pipeline_passes;
    @field_list(1)
    bit<3>  i;       //[RECIRC] (i+1) indicates the current component position in the LNPM process.
    @field_list(1)
    bit<8>  SwId;    //[RECIRC] it stores the latest switch id for the lnpm process..
    @field_list(1)
    bit<4>  max;     //[RECIRC] it stores the max number of components obtained by the DPST match table
    bit<64> name_shape;
    bit<1>  dpst_hit;
    bit<1>  cpst_hit;
    bit<3>  hit_status;   //1=table is not at ingress. 2=table is at ingress but miss. 3=table is at ingress and hits.
                          //5=table is not at egress. 2=table is at egress but miss. 3=table is at egress and hits.
    bit<1>  finish;     //finish 1 -> LNPM has finished
    bit<1>  mac_hit;
    bit<8>  table_id;

    //the field instance_type in this struct will store a copy of the
    //standard_metadata.instance_type to allow us to identify whether a packet
    //is NORMAL or RECIRCULATE at the egress. For some reason, the
    //standard_metadata.instance_type is not keeping its value from ingress to egress
    bit<4>  instance_type;

    bit<32> crc32;
    bit<32> Fx;
    bit<1>  c;      //continue bit
    bit<1>  e;      //ending bit
    bit<1>  cf;     //conflicting bit
    bit<3>  pos;    //component position. pos=000 is position 1. pos=111 is position 8
    bit<8>  sw_id;  //current switch id given by each table matching
    bit<10> hs;
}

struct headers {
    ethernet_t   ethernet;
    num_passes_t number_passes;
    num_comp_t   number_elements;
    tuple_t[8]   elements;
    num_comp_t   number_elements_copy;
    tuple_t[8]   elements_copy;
    ipv4_t       ipv4;
}


/*************************************************************************
*********************** P A R S E R  ***********************************
*************************************************************************/

parser MyParser(packet_in packet,
                out headers hdr,
                inout metadata meta,
                inout standard_metadata_t standard_metadata) {

    bit<8> temp = 0;

    state start {
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            TYPE_IPV4:    parse_ipv4;
            TYPE_FANTNET: parse_num;
            default: accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition accept;
    }

    state parse_num {
    	packet.extract(hdr.number_elements);
    	temp = hdr.number_elements.n;
    	transition parse_elements;
    }

    state parse_elements {
    	transition select(temp) {
    		0       : accept;
    		default : parse_element;
    	}
    }

    state parse_element {
    	packet.extract(hdr.elements.next);
    	temp = temp - 1;
    	transition parse_elements;
    }
}

/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control MyVerifyChecksum(inout headers hdr, inout metadata meta) {
    apply {  }
}


/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyIngress(inout headers hdr,
                  inout metadata meta,
                  inout standard_metadata_t standard_metadata) {

    action hct_hit(bit<8> swId) {    
           if(swId != 0)
              meta.SwId = swId;
    }

    action shape_dp_hit(bit<4> number_name_components) {
    	meta.dpst_hit = 1;
    	meta.max = number_name_components;
    }

    action shape_cp_hit(bit<4> number_name_components) {
    	meta.cpst_hit = 1;
    }

    action create_shapes() {
    	if(hdr.elements[0].isValid()){
    		meta.name_shape[63:56] = hdr.elements[0].table_id;
    	} else {
    		meta.name_shape[63:00] = 64w0;
    		return;
    	}
    	if(hdr.elements[1].isValid()){
    		meta.name_shape[55:48] = hdr.elements[1].table_id;
    	} else {
    		meta.name_shape[55:00] = 56w0;
    		return;
    	}
    	if(hdr.elements[2].isValid()){
    		meta.name_shape[47:40] = hdr.elements[2].table_id;
    	} else {
    		meta.name_shape[47:00] = 48w0;
    		return;
    	}
    	if(hdr.elements[3].isValid()){
    		meta.name_shape[39:32] = hdr.elements[3].table_id;
    	} else {
    		meta.name_shape[39:00] = 40w0;
    		return;
    	}
    	if(hdr.elements[4].isValid()){
    		meta.name_shape[31:24] = hdr.elements[4].table_id;
    	} else {
    		meta.name_shape[31:00] = 32w0;
    		return;
    	}
    	if(hdr.elements[5].isValid()){
    		meta.name_shape[23:16] = hdr.elements[5].table_id;
    	} else {
    		meta.name_shape[23:00] = 24w0;
    		return;
    	}
    	if(hdr.elements[6].isValid()){
    		meta.name_shape[15:8] = hdr.elements[6].table_id;
    	} else {
    		meta.name_shape[15:00] = 16w0;
    		return;
    	}
    	if(hdr.elements[7].isValid()){
    		meta.name_shape[7:0] = hdr.elements[7].table_id;
    	} else {
    		meta.name_shape[7:00] = 8w0;
    	}
    }


    action drop() {
        mark_to_drop(standard_metadata);
    }

    action get_cad(bit<24> cad){
    	meta.hit_status = INGRESS_HIT;
        meta.c          = cad[23:23];
        meta.e          = cad[22:22];
        meta.cf         = cad[21:21];
        meta.pos        = cad[20:18];
    	meta.sw_id      = cad[17:10];
    	meta.hs         = cad[9:0];
    }

    action mac_forward(egressSpec_t port) {
        meta.mac_hit = 1;
	standard_metadata.egress_spec = port;
    }

    action ipv4_forward(macAddr_t dstAddr, egressSpec_t port) {
        standard_metadata.egress_spec = port;
        hdr.ethernet.srcAddr = hdr.ethernet.dstAddr;
        hdr.ethernet.dstAddr = dstAddr;
        hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
    }

    action send_to_cpu(){
    	standard_metadata.egress_spec = CPU;
    }

    action speculate_output_port() {

        //let's assume that the prefix /a/b exists in CoFIB but the prefix /a does not.
        //let's also assume that a given ipckt comes with the prefix /a/b.
        //the component 'a' will match at ingress and the SwId is set to 0.
        //since we can not set the output port at the egress, we have to speculate the output port
        //here at the ingress. in case where the second component 'b' miss at the egress or cad.e{b}==0,
        //we drop the packet at the egress. this action is suppose to override the output port previously set.

        bit<9> output_port;
    	random(output_port, 2, 3);
    	//speculating the output port
    	standard_metadata.egress_spec = output_port;
    }

    action add_sw_id_to_packet(){
    	//This action is called when a packet need to be sent do the core switch
    	//This action consists of adding a SwId value and a number of pipeline passes into the packet header
    	//One possibility to do that is to add SwId into a customized header.
    	//However, it is possible that some core switches are not programmable and will not recognize such field.
    	//Therefore, we add SwId into the destination mac address field in the ethernet frame.

    	//adding SwId to destination mac address in the ethernet frame
    	hdr.ethernet.srcAddr = 48w0;
    	hdr.ethernet.dstAddr = 40w0 ++ meta.SwId;
    	
    	//adding the current number of pipeline passes into the packet header
    	hdr.number_passes.setValid();
    	hdr.number_passes.p = meta.pipeline_passes;

    	//Adding SwId into a customized header..
    	//hdr.sw_id.setValid();
    	//hdr.sw_id.marker = 16w0;
    	//hdr.sw_id.SwId   = meta.SwId;
    }

    table mac {
    	key = {
    	    hdr.ethernet.dstAddr: exact;
    	}
    	actions = {
    	    mac_forward;
    	    drop;
    	    NoAction;
    	}
    	size = 100;
    	default_action = drop();
    }

    table ipv4_lpm {
        key = {
            hdr.ipv4.dstAddr: lpm;
        }
        actions = {
            ipv4_forward;
            drop;
            NoAction;
        }
        size = 1024;
        default_action = drop();
    }


    table tHCT {
    	key = {
    		meta.Fx : exact;
    	}
    	actions = {
    		hct_hit;
    		NoAction;
    	}
    	size = 100000;
    	default_action = NoAction();
    }


    table tDPST {
    	key = {
    		meta.name_shape : lpm;
    	}
    	actions = {
    		shape_dp_hit;
    		NoAction;
    	}
    	size = 100000;
    	default_action = NoAction();
    }

    table tCPST {
    	key = {
    		meta.name_shape : lpm;
    	}
    	actions = {
    		shape_cp_hit;
    		NoAction;
    	}
    	size = 1000;
    	default_action = NoAction();
    }

    table t2 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 583;
    }

    table t3 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 10835;
    }

    table t5 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 94591;
    }

    table t8 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 297129;
    }

    table t11 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 387675;
    }

    table t13 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 353666;
    }
    
    
    table t15 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 275755;
    }
    
    table t16 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 227957;
    }

    table t17 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 184988;
    }
    
    
    table t22 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 50051;
    }
    
    
    table t23 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 36212;
    }
    
    table t24 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 26568;
    }
    
    table t29 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	}
    	size = 4906;
    }

    apply {

        //Saving the copy of instance_type to be acessible at egress
	meta.instance_type = (bit<4>)standard_metadata.instance_type;

        //At the ingress, I can use the field standard_metadata.instance_type normally
        //However, at the egress, I have to use meta.instance_type instead, because the standard_metadata.instance_type
        //does not hold its value from ingress to egress. This is an architectural issue, I think..
        if(standard_metadata.instance_type == NORMAL) {

        	 //if the mac source address is 0 means that this packet was previously processed in CoFIB at some edge switch
        	 //then, we need to apply the mac table to extract the correct output port to send the packet torwards the SwId destination edge switch
                if(hdr.ethernet.isValid()){
                	if(hdr.ethernet.srcAddr == 48w0){
                		mac.apply();
                		return;
                	}
                }

        	//if the ipv4 header is valid, such a packet will be forwarded according to the ip table
        	if(hdr.ipv4.isValid()) {
        		ipv4_lpm.apply();
        		return;
        	}

        	//if the two previous cases does not hold, means that the current packet should be processed in CoFIB
        	if(hdr.number_elements.isValid()){
        	        meta.pipeline_passes = 1;
        		meta.i = 0;
        		create_shapes();
        		tDPST.apply();
        		tCPST.apply();
        	}
        	if(meta.dpst_hit == 0) {
        		meta.finish = 1;
        		if(meta.cpst_hit == 1) {
        			send_to_cpu();
        			return;
        		} else {
        			drop();
        			return;
        		}
        	}
        }

	if(standard_metadata.instance_type == RECIRC ||
	   (standard_metadata.instance_type == NORMAL && meta.dpst_hit == 1)) {

	   if (hdr.number_elements.isValid()) {
             if(hdr.elements[0].isValid()){
               //check if table is in this stage
               if(hdr.elements[0].table_id==2 || hdr.elements[0].table_id==3 || hdr.elements[0].table_id==5 || hdr.elements[0].table_id==8 || hdr.elements[0].table_id==11 || hdr.elements[0].table_id==13 ||
                  hdr.elements[0].table_id==15 || hdr.elements[0].table_id==16 || hdr.elements[0].table_id==17 || hdr.elements[0].table_id==22 || hdr.elements[0].table_id==23 ||
                  hdr.elements[0].table_id==24 || hdr.elements[0].table_id==29) {
                 
                 meta.table_id = hdr.elements[0].table_id;
                 meta.crc32    = hdr.elements[0].crc32;
                 meta.Fx       = hdr.elements[0].Fx;
	         switch(meta.table_id) {
	    	   2 : {t2.apply();}
	    	   3 : {t3.apply();}
	    	   5 : {t5.apply();}
	    	   8 : {t8.apply();}
	    	   11 : {t11.apply();}
	    	   13 : {t13.apply();}
	    	   15 : {t15.apply();}
	    	   16 : {t16.apply();}
	    	   17 : {t17.apply();}
	    	   22 : {t22.apply();}
	    	   23 : {t23.apply();}
	    	   24 : {t24.apply();}
	    	   29 : {t29.apply();}
	         }
	         if(meta.hit_status == INGRESS_HIT) {
	            if(meta.cf == 1){
	               if(!tHCT.apply().hit) {
	                  meta.finish = 1;
	                  drop();
	                  return;
	               } else {
	                 //handle_further_processing();
	                 //meta.cf == 1
	                 if (meta.pos == meta.i) {
    			    if(meta.c == 1) {

    			       //I think I can not update the field meta.SwId if meta.e == 1 at this point.
    			       //why? because as meta.cf == 1, the meta.SwId at this point was taken
    			       //from the HCT table and it should be used and not be overrided at all

    			       /*
    			       if(meta.e == 1) {
    				  meta.SwId = meta.sw_id;
    			       }
    			       */

    			       //set_packet_fate();

    			       bit<3> max = (bit<3>) (meta.max-1);
    	           	       if(meta.i < max) {
    	           	          meta.i = meta.i + 1;
    		                  //speculate output port at the end of this action
    	                       } else {
    	                          meta.finish = 1;
    	                          add_sw_id_to_packet();
    	                          //speculate output port at the end of this action
    	                       }

    		            } else {
    		               //cf == 1 at this point
    		               //the meta.SwId value obtained in hct_hit must be keeped because
    		               //it has priority over the meta.sw_id value obtained in get_cad
    		               //therefore, I cant do meta.SwId = meta.sw_id;
    		               meta.finish = 1;
    			       add_sw_id_to_packet();
    			       //speculate output port at the end of this action
    		            }
    	                 } else {
    	                    meta.finish=1;
    		            if(standard_metadata.instance_type == NORMAL) {
    			       if(meta.cpst_hit == 1){
    				  send_to_cpu();
    				  return;
    			       } else {
    				  drop();
    				  return;
    			       }
    		            } else {
    			       if(meta.SwId != 0) {
    				  add_sw_id_to_packet();
    				  //speculate output port at the end of this action
    			       } else {
    				  drop();
    				  return;
    			       }
    		            }
    	                }
	               }
	            } else {
	            	//meta.cf == 0
	            	if(meta.i > 0) {
	            	   //2th position or more. meta.i==0 means position 1 (first position)
	            	   bit<10> hs = meta.Fx[9:0];
	            	   if(meta.hs == hs) {
	            	      //handle_further_processing();

	            	      if (meta.pos == meta.i) {
    			         if(meta.c == 1) {
    			            if(meta.e == 1) {
    				       meta.SwId = meta.sw_id;
    			            }
    			            //set_packet_fate();

    			            bit<3> max = (bit<3>) (meta.max-1);
    	           	            if(meta.i < max){
    	           	               meta.i = meta.i + 1;
    		                       //speculate output port at the end of this action
    	                            } else {
    	                               meta.finish=1;
    		                       add_sw_id_to_packet();
    		                       //speculate output port at the end of this action
    	                            }
    		                 } else {
    		                   //meta.cf == 0
    		                   meta.finish=1;
    		                   //here, since meta.cf is 0, the meta.SwId must be updated with the value
    		                   //meta.sw_id obtained in get_action
    			           meta.SwId = meta.sw_id;
    			           add_sw_id_to_packet();
    			           //speculate output port at the end of this action
    		                 }
    	                      } else {
    	                         meta.finish=1;
    		                 if(standard_metadata.instance_type == NORMAL) {
    			            if(meta.cpst_hit == 1){
    				       send_to_cpu();
    				       return;
    			            } else {
    				       drop();
    				       return;
    			            }
    		                 } else {
    			            if(meta.SwId != 0) {
    				       add_sw_id_to_packet();
    				       //speculate output port at the end of this action
    			            } else {
    				       drop();
    				       return;
    			            }
    		                 }
    	                      }
	            	   } else {
	            	      meta.finish = 1;
	            	      if(meta.SwId == 0) {
	            	      	drop();
	            	      	return;
	            	      } else {
	            	      	add_sw_id_to_packet();
	            	      	//speculate output port at the end of this action
	            	      }
	            	   }
	            	} else {
	            	  //meta.i == 0 (name component is at the first position)
	            	  //handle_further_processing();

	            	  if (meta.pos == meta.i) {
    			      if(meta.c == 1) {
    			         if(meta.e == 1) {
    				    meta.SwId = meta.sw_id;
    			         }
    			         //set_packet_fate();

    			         bit<3> max = (bit<3>) (meta.max-1);
    	           	         if(meta.i < max){
    	           	            meta.i = meta.i + 1;
    		                    //speculate output port at the end of this action
    	                         } else {
    	                            meta.finish=1;
    		                    add_sw_id_to_packet();
    		                    //speculate output port at the end of this action
    	                         }
    		              } else {
    		                 meta.finish=1;
    			         meta.SwId = meta.sw_id;
    			         add_sw_id_to_packet();
    			         //speculate output port at the end of this action
    		              }
    	                  } else {
    	                      meta.finish=1;
    		              if(standard_metadata.instance_type == NORMAL) {
    			         if(meta.cpst_hit == 1){
    				    send_to_cpu();
    				    return;
    			         } else {
    				    drop();
    				    return;
    			         }
    		              } else {
    			         if(meta.SwId != 0) {
    				    add_sw_id_to_packet();
    				    //speculate output port at the end of this action
    			         } else {
    				    drop();
    				    return;
    			         }
    		              }
    	                  }
	            	}
	            }
	         } else {
	             //meta.hit_status != INGRESS_HIT
	             meta.finish=1;
	             meta.hit_status=INGRESS_MISS;
	             if(standard_metadata.instance_type == NORMAL) {
	             	if(meta.cpst_hit == 1) {
	             	   send_to_cpu();
	             	   return;
	             	} else {
	             	   drop();
	             	   return;
	             	}
	             } else {
	               //packet is not NORMAL
	               if(meta.SwId == 0) {
	                  drop();
	                  return;
	               } else {
	                  add_sw_id_to_packet();
	                  //speculate output port at the end of this action
	               }
	             }
	         }
               } else {
                  //table is not at this stage
                  meta.hit_status=NOT_AT_INGRESS;
                  //speculate output port at the end of this action
               }
             } else {
             	//hdr.elements[0] is not valid
             	meta.finish=1;
             }
           } else {
              //hdr.number_elements is not valid
              meta.finish=1;
             }
        } else {
           //unreachable condition
           meta.finish=1;
        }

        speculate_output_port();

    } //end apply
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyEgress(inout headers hdr,
                 inout metadata meta,
                 inout standard_metadata_t standard_metadata) {
    
    action add_sw_id_to_packet(){
    	//This action is called when a packet need to be sent do the core switch
    	//This action consists of adding a SwId value and a number of pipeline passes into the packet header
    	//One possibility to do that is to add SwId into a customized header.
    	//However, it is possible that some core switches are not programmable and will not recognize such field.
    	//Therefore, we add SwId into the destination mac address field in the ethernet frame.

    	//adding SwId to destination mac address in the ethernet frame
    	hdr.ethernet.srcAddr = 48w0;
    	hdr.ethernet.dstAddr = 40w0 ++ meta.SwId;
    	
    	//adding the current number of pipeline passes into the packet header
    	hdr.number_passes.setValid();
    	hdr.number_passes.p = meta.pipeline_passes;

    	//Adding SwId into a customized header..
    	//hdr.sw_id.setValid();
    	//hdr.sw_id.marker = 16w0;
    	//hdr.sw_id.SwId   = meta.SwId;
    }

    action remove_first_name_component() {
    	hdr.number_elements.n = hdr.number_elements.n - 1;
    	hdr.elements[0].setInvalid();
    }

    action remove_first_two_name_components() {
    	hdr.number_elements.n = hdr.number_elements.n - 2;
    	hdr.elements[0].setInvalid();
    	hdr.elements[1].setInvalid();
    }

    action make_copy_headers() {
        hdr.number_elements_copy = hdr.number_elements;
    	hdr.elements_copy = hdr.elements;
    }

    action remove_headers() {
    	hdr.number_elements.setInvalid();
    	hdr.elements[0].setInvalid();
    	hdr.elements[1].setInvalid();
    	hdr.elements[2].setInvalid();
    	hdr.elements[3].setInvalid();
    	hdr.elements[4].setInvalid();
    	hdr.elements[5].setInvalid();
    	hdr.elements[6].setInvalid();
    	hdr.elements[7].setInvalid();
    }

    action drop() {
        mark_to_drop(standard_metadata);
    }

    action hct_hit(bit<8> swId) {        
           if(swId != 0)
              meta.SwId = swId;
    }

    action get_cad(bit<24> cad) {
        meta.hit_status = EGRESS_HIT;
        meta.c     = cad[23:23];
        meta.e     = cad[22:22];
        meta.cf    = cad[21:21];
        meta.pos   = cad[20:18];
    	meta.sw_id = cad[17:10];
    	meta.hs    = cad[9:0];
    }

    table tHCT {
    	key = {
    		meta.Fx : exact;
    	}
    	actions = {
    		hct_hit;
    		NoAction;
    	}
    	size = 100000;
    	default_action = NoAction();
    }

    table t1 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 24;
    }

    table t4 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 41975;
    }

    table t6 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 169081;
    }

    table t7 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 238469;
    }
    
    table t9 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 340588;
    }

    
    table t10 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 378556;
    }
    
    table t12 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 381207;
    }
    
    table t14 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 319036;
    }
    
    table t18 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 148193;
    }
    
    table t19 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 116953;
    }
    
    table t20 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 90294;
    }
    
    table t21 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 66881;
    }
    
    table t25 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 19226;
    }
    
    table t26 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 13532;
    }
    
    table t27 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 11474;
    }
    
    table t28 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 6802;
    }
    
    table t30 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 3509;
    }
    
    table t31 {
    	key = {
    	    meta.crc32: exact;
    	}
    	actions = {
    	    get_cad;
    	    drop;
    	}
    	size = 2328;
    }

    apply {
	
      if(hdr.ethernet.isValid()){
         if(hdr.ethernet.srcAddr == 48w0 && meta.mac_hit == 1) {
             return;
         }
      }

      if(hdr.ipv4.isValid())
      	return;


      bit<1> table_lookup = 0;
      bit<1> not_ingress  = 0;
      bit<1> ingress_hit  = 0;

      if(meta.finish == 0) {
        //it means that the LNPM has not been finish yet
        if(meta.hit_status == NOT_AT_INGRESS) {
          //it means that the current component is NOT at ingress
    	  //at this point, both hdr.elements[0] and hdr.number_elements is valid
    	  //otherwise meta.finish will be set to 1
    	  
    	  //check if table is in this stage
          if(hdr.elements[0].table_id==1 || hdr.elements[0].table_id==4 || hdr.elements[0].table_id==6 || hdr.elements[0].table_id==7 || hdr.elements[0].table_id==9 || hdr.elements[0].table_id==10 ||
             hdr.elements[0].table_id==12 || hdr.elements[0].table_id==14 || hdr.elements[0].table_id==18 || hdr.elements[0].table_id==19 || hdr.elements[0].table_id==20 ||
             hdr.elements[0].table_id==21 || hdr.elements[0].table_id==25 || hdr.elements[0].table_id==26 || hdr.elements[0].table_id==27 || hdr.elements[0].table_id==28 ||
             hdr.elements[0].table_id==30 || hdr.elements[0].table_id==31) {
    	     //the current component IS at egress
    	     meta.table_id = hdr.elements[0].table_id;
             meta.crc32    = hdr.elements[0].crc32;
             meta.Fx       = hdr.elements[0].Fx;
             table_lookup  = 1;
             not_ingress   = 1;
    	  } else {
    	     //at this point, the table does not exist in both ingress and egress
    	     //this situation is not suppose to happen
    	     drop();
    	     return;
    	  }
        } else {
          if(meta.hit_status == INGRESS_HIT) {
    	    //it means that the current name component hits at ingress and we need to perform a matching on the next element.
    	    //at this point, both hdr.elements[0] and hdr.number_elements is valid
    	    //otherwise meta.finish will be set to 1

    	    //since hdr.elements[0] matching hits at the ingress, we perform the matching using
    	    //the second name component hdr.elements[1].
    	    //however, the hdr.elements[1] might be invalid at this point and we need to check it out
    	    if(hdr.elements[1].isValid()) {
    	      //check if table is in this stage
             if(hdr.elements[1].table_id==1  || hdr.elements[1].table_id==4  || hdr.elements[1].table_id==6  || hdr.elements[1].table_id==7  || hdr.elements[1].table_id==9  || 
                hdr.elements[1].table_id==10 || hdr.elements[1].table_id==12 || hdr.elements[1].table_id==14 || hdr.elements[1].table_id==18 || hdr.elements[1].table_id==19 || 
                hdr.elements[1].table_id==20 || hdr.elements[1].table_id==21 || hdr.elements[1].table_id==25 || hdr.elements[1].table_id==26 || hdr.elements[1].table_id==27 || 
                hdr.elements[1].table_id==28 || hdr.elements[1].table_id==30 || hdr.elements[1].table_id==31) {
    	      
    	    	 meta.table_id = hdr.elements[1].table_id;
                 meta.crc32    = hdr.elements[1].crc32;
                 meta.Fx       = hdr.elements[1].Fx;
                 table_lookup  = 1;
                 ingress_hit   = 1;
              } else {
                 // it means that the name component in element[0] is at ingress and the element[1] is also in the ingress
                 // here I have to check if the packet is NORMAL or not. If it is NORMAL, I have to make copy of the headers.
                 // since it is not possible to keep the value of standard_metadata.instance_type from ingress to egress,
                 // I have to use the field meta.instance_type instead of standard_metadata.instance_type to check if the
                 // packet is NORMAL. The copy was made at ingress
    	         if(meta.instance_type == NORMAL) {
    	            make_copy_headers();
    	         }    	         

                 meta.pipeline_passes = meta.pipeline_passes + 1;
    	         remove_first_name_component();
    	         //we have to recirculate the packet
    	         recirculate_preserving_field_list(1);
    	         return;                
              }
            } else {
              //the LNPM process has finished
    	      //since the previous name component has matched at the ingress (meta.hit_status == INGRESS_HIT)
    	      //the packet is suppose to be sent to the output port set at the ingress if SwId != 0 or be dropped otherwise
    	      if(meta.SwId == 0){
    	         drop();
    	      }
    	      return;
            }

    	  } else {
    	     //meta.hit_status == INGRESS_MISS is unreachable at this point because meta.finish==0s
    	  }
        }
      } else {
        //it means that the LNPM has finish (meta.finish==1)
      }

      /*
      	 We perform a table lookup only if the table_lookup is set to 1 (table_lookup == 1)

         The only two ways table_lookup is set to 1 at this point is:

         1) meta.finish==0  &&  meta.hit_status == NOT_AT_INGRESS  &&                                  (hdr.elements[0].table_id>=5 && hdr.elements[0].table_id<=8), or
         2) meta.finish==0  &&  meta.hit_status == INGRESS_HIT     &&  hdr.elements[1].isValid()  &&   (hdr.elements[1].table_id>=5 && hdr.elements[1].table_id<=8)

       */


      if(table_lookup == 1) {
        switch(meta.table_id) {
	   1  : {t1.apply();}
	   4  : {t4.apply();}
	   6  : {t6.apply();}
	   7  : {t7.apply();}
	   9  : {t9.apply();}
	   10 : {t10.apply();}
	   12 : {t12.apply();}
	   14 : {t14.apply();}
	   18 : {t18.apply();}
	   19 : {t19.apply();}
	   20 : {t20.apply();}
	   21 : {t21.apply();}
	   25 : {t25.apply();}
	   26 : {t26.apply();}
	   27 : {t27.apply();}
	   28 : {t28.apply();}
	   30 : {t30.apply();}
	   31 : {t31.apply();}	    
	}

	if(meta.hit_status == EGRESS_HIT) {
	   if(meta.cf == 1) {
	      if(!tHCT.apply().hit) {
	         meta.finish = 1;
	         drop();
	         return;
	      } else {
	         //meta.cf == 1
	         if (meta.pos == meta.i) {
    		    if(meta.c == 1) {

    		       //Likewise at the ingress, here I think I can not update the field meta.SwId if meta.e == 1 at this point.
    		       //why? because as meta.cf == 1, the meta.SwId at this point was taken
    		       //from the HCT table and it should be used and not be overrided at all

    		       /*
    	               if(meta.e == 1) {
    	                  meta.SwId = meta.sw_id;
    		       }
    		       */

    		       bit<3> max = (bit<3>) (meta.max-1);
    	               if(meta.i < max) {
    	                     // at this point, we dont know if there was a lookup in ingress or not. we need to check it out
    	           	     if(not_ingress == 1) {
    	           	        if(hdr.number_elements.n > 1) {
    	           	            meta.i = meta.i + 1;
    	           	     	      // we need to remove the first name component because at this point
    	           	            // not_ingress == 1 and meta.hit_status == EGRESS_HIT
    	           	            if(meta.instance_type == NORMAL) {
    	                            make_copy_headers();
    	                        }
                                meta.pipeline_passes = meta.pipeline_passes + 1;
    	           	        remove_first_name_component();
    	           	        // we have to recirculate the packet preserving some fields
    	                        recirculate_preserving_field_list(1);
    	                        return;
                               
    	           	        } else {
    	           	            meta.finish=1;
    	           	            //all components in the name was lookup and we dont need to recirculate
    	           	            if(meta.SwId == 0) {
    	           	            	drop();
    	           	            	return;
    	           	            } else {
    	           	                //just let the packet be sent to the speculate port set at ingress if SwId != 0
    	           	                add_sw_id_to_packet();
    		                        //remove headers only if the packet is recirculated..
                                        //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                                if(meta.instance_type == RECIRC)
    	                                    remove_headers();
    	           	            }
    	           	        }
    	           	     } else {
    	           	        if(ingress_hit == 1) {
    	           	           if(hdr.number_elements.n > 2) {
    	           	               meta.i = meta.i + 1;
    	           	               // we need to remove the first TWO name component because at this point
    	           	               // ingress_hit == 1 and meta.hit_status == EGRESS_HIT, therefore, we performed two matches at this pipeline pass
    	           	               if(meta.instance_type == NORMAL) {
    	                                   make_copy_headers();
    	                           }
                                 
    	           	           meta.pipeline_passes = meta.pipeline_passes + 1;
    	           	           remove_first_two_name_components();
    	           	           // we have to recirculate the packet
    	                           recirculate_preserving_field_list(1);
    	                           return;
                                  
    	           	           } else {
    	           	               meta.finish=1;
    	           	               //all components in the name was lookup and we dont need to recirculate
    	           	               if(meta.SwId != 0) {
    	           	                  //just let the packet be sent to the speculate port set at ingress if SwId != 0
    	           	                  add_sw_id_to_packet();
    		                          //remove headers only if the packet is recirculated..
                                          //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                                  if(meta.instance_type == RECIRC)
    	                                     remove_headers();
    	           	               } else {
    	           	                  drop();
    	           	            	  return;
    	           	               }
    	           	           }
    	           	        } else {
    	           	           //this situation indicates that EGRESS hit, table is at ingress, and ingress_hit == 0
    	           	           //that`s not possible. for instance, it is not possible to have miss at ingress and hit at egress
    	           	           drop();
    	           	           return;
    	           	        }
    	           	     }
    	               } else {
    	                  //meta.i == max  (LNPM has finish)
    	                  meta.finish = 1;
    	                  if(meta.SwId != 0) {
    	                     //let the packet be sent to the speculate port set at ingress
    	                     add_sw_id_to_packet();
    	                     //remove headers only if the packet is recirculated..
                             //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                     if(meta.instance_type == RECIRC)
    	                         remove_headers();
    	                     return;
    	                  } else {
    	                     drop();
    	                     return;
    	                  }
    	               }
    		    } else {
    		       //cf == 1 at this point

    		       //its implict that cad.e == 1.
    		       if(meta.e == 0) {
    		          //this situation is not suppose to happen
    		          drop();
    		          return;
    		       }
    		       //let the packet be sent to the speculate port set at ingress
    		       meta.finish = 1;
    		       //since cf == 1, I cant update meta.SwId with meta.sw_id at this point
    		       //The meta.SwId value obtained from action hct_hit must be keeped and has priority over the
    		       //meta.sw_id value obtained in get_cad
    		       add_sw_id_to_packet();
    		       //remove headers only if the packet is recirculated..
    		       //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    		       if(meta.instance_type == RECIRC)
    		           remove_headers();
    		    }
    	         } else {
    	            meta.finish=1;
    		    if(meta.SwId != 0) {
    		       //it is possible to send a packet when (meta.pos != meta.i) as long as the SwId != 0
    		       add_sw_id_to_packet();
    		       //remove headers only if the packet is recirculated..
                       //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	               if(meta.instance_type == RECIRC)
    	                   remove_headers();
    	            } else {
    		       drop();
    		       return;
    	            }
    	         }
	      }
	   } else {
	      //meta.cf == 0
	      if(meta.i > 0) {
	         //2th position or more. meta.i==0 means position 1 (first position)
	         //bit<10> hs = (bit<10>) (meta.Fx >> 22);
	         bit<10> hs = meta.Fx[9:0];
	         if(meta.hs == hs) {
	            if (meta.pos == meta.i) {
    		       if(meta.c == 1) {
    		          if(meta.e == 1) {
    			     meta.SwId = meta.sw_id;
    			  }
    			  bit<3> max = (bit<3>) (meta.max-1);
    	           	  if(meta.i < max) {
    	           	     // at this point, we dont know if there was a lookup in ingress or not. we need to check it out
    	           	     if(not_ingress == 1) {
    	           	        if(hdr.number_elements.n > 1) {
    	           	            meta.i = meta.i + 1;
    	           	     	    // we need to remove the first name component because at this point
    	           	            // not_ingress == 1 and meta.hit_status == EGRESS_HIT
    	           	            if(meta.instance_type == NORMAL) {
    	                               make_copy_headers();
    	                        }
                              
                                    meta.pipeline_passes = meta.pipeline_passes + 1;
    	           	            remove_first_name_component();
    	           	            // we have to recirculate the packet
    	                            recirculate_preserving_field_list(1);
    	                            return;
                               
    	           	        } else {
    	           	            meta.finish=1;
    	           	            //all components in the name was lookup and we dont need to recirculate
    	           	            if(meta.SwId == 0) {
    	           	            	drop();
    	           	            	return;
    	           	            } else {
    	           	                //just let the packet be sent to the speculate port set at ingress if SwId != 0
    	           	                add_sw_id_to_packet();
    		                        //remove headers only if the packet is recirculated..
                                        //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                                if(meta.instance_type == RECIRC)
    	                                     remove_headers();
    	           	            }
    	           	        }
    	           	     } else {
    	           	        if(ingress_hit == 1) {
    	           	           if(hdr.number_elements.n > 2) {
    	           	               meta.i = meta.i + 1;
    	           	               // we need to remove the first TWO name component because at this point
    	           	               // ingress_hit == 1 and meta.hit_status == EGRESS_HIT, therefore, we performed two matches at this pipeline pass
    	           	               if(meta.instance_type == NORMAL) {
    	                                  make_copy_headers();
    	                           }
                                 
                                        meta.pipeline_passes = meta.pipeline_passes + 1;
    	           	                remove_first_two_name_components();
    	           	                // we have to recirculate the packet
    	                               recirculate_preserving_field_list(1);
    	                               return;
                                  
    	           	           } else {
    	           	               meta.finish=1;
    	           	               //all components in the name was lookup and we dont need to recirculate
    	           	               if(meta.SwId != 0) {
    	           	                  //just let the packet be sent to the speculate port set at ingress if SwId != 0
    	           	                  add_sw_id_to_packet();
    	           	                  //remove headers only if the packet is recirculated..
                                          //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                                  if(meta.instance_type == RECIRC)
    	                                      remove_headers();
    	           	               } else {
    	           	                  drop();
    	           	            	  return;
    	           	               }
    	           	           }
    	           	        } else {
    	           	           //this situation indicates that EGRESS hit, table is at ingress, and ingress_hit == 0
    	           	           //that`s not possible. for instance, it is not possible to have miss at ingress and hit at egress
    	           	           drop();
    	           	           return;
    	           	        }
    	           	     }
    	                  } else {
    	                     //meta.i == meta.max   (the LNPM has finish)
    	                     meta.finish=1;
    	                     if(meta.SwId != 0) {
    		                add_sw_id_to_packet();
    		                //remove headers only if the packet is recirculated..
                                //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                        if(meta.instance_type == RECIRC)
    	                           remove_headers();
    		                //let the packet be sent to the speculate port set at ingress
    		             } else {
    		                //if the LNPM finish and the SwId == 0, we have to discart the packet
    		                drop();
    		                return;
    		             }
    	                  }
    		       } else {
    		           // cf == 0 at this point
    		           //its implicit that cad.e == 1
    		           if(meta.e == 0) {
    		               drop();
    		               return;
    		           }
    		           meta.finish=1;
    		           //here, since meta.cf is 0, the meta.SwId must be updated with the value
    		           //meta.sw_id obtained in get_action
    			   meta.SwId = meta.sw_id;
    			   add_sw_id_to_packet();
    			   //remove headers only if the packet is recirculated..
                           //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                  if(meta.instance_type == RECIRC)
    	                      remove_headers();
    		       }
    	            } else {
    	                //position is different (meta.pos != meta.i)
    		        meta.finish=1;
    		        if(meta.SwId != 0) {
    		            //it is possible to send a packet when (meta.pos != meta.i) as long as the SwId != 0
    			    add_sw_id_to_packet();
    			    //remove headers only if the packet is recirculated..
                           //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                   if(meta.instance_type == RECIRC)
    	                        remove_headers();
    			} else {
    			    drop();
    			    return;
    			}
    	            }
	         } else {
	            // meta.hs != hs
	            meta.finish = 1;
	            if(meta.SwId == 0) {
	            	drop();
	              	return;
	            } else {
	              	add_sw_id_to_packet();
	              	//remove headers only if the packet is recirculated..
                       //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	               if(meta.instance_type == RECIRC)
    	                   remove_headers();
	            }
	         }
	      } else {
	         //meta.i == 0 (name component is at the first position)
	         //handle_further_processing();

	         //meta.i == 0 at this point means that the table does not exist in ingress
	         //the flag not_ingress must be 1 at this point
	         if(not_ingress != 1) {
	                //this situation is not suppose to happen (meta.i == 0) and (not_ingress == 0)
	         	drop();
	         	return;
	         }

	         if (meta.pos == meta.i) {
    		    if(meta.c == 1) {
    		       if(meta.e == 1) {
    		          meta.SwId = meta.sw_id;
    		       }
    		       bit<3> max = (bit<3>) (meta.max-1);
    	               if(meta.i < max){
    	           	      meta.i = meta.i + 1;
    	           	      // we need to remove the first name component because at this point
    	           	      // not_ingress == 1 and meta.hit_status == EGRESS_HIT, therefore,
    	           	      if(meta.instance_type == NORMAL) {
    	                      make_copy_headers();
    	                  }
                        
    	           	      meta.pipeline_passes = meta.pipeline_passes + 1;
    	           	      remove_first_name_component();
    	                     // we have to recirculate the packet
    	                     recirculate_preserving_field_list(1);
    	                     return;
                         
    	               } else {
    	                   //just let the packet be sent to the speculate port set at ingress
    	                   meta.finish=1;
    		           add_sw_id_to_packet();
    		           //remove headers only if the packet is recirculated..
                           //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	                  if(meta.instance_type == RECIRC)
    	                       remove_headers();
    		          return;
    		       }
    		    } else {
    		       meta.finish=1;
    		       meta.SwId = meta.sw_id;
    		       add_sw_id_to_packet();
    		       //remove headers only if the packet is recirculated..
                       //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	               if(meta.instance_type == RECIRC)
    	                   remove_headers();
    		    }
    	         } else {
    	            meta.finish=1;
    	            if(meta.SwId != 0) {
    		       add_sw_id_to_packet();
    		        //remove headers only if the packet is recirculated..
                       //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	               if(meta.instance_type == RECIRC)
    	                   remove_headers();
    		    } else {
    		       drop();
    		       return;
    		    }
    	         }
	      }
	   }
	} else {

	   //  if egress lookup fails at this point, it means that:
	   //     - table is not at ingress and missed at egress
	   //     - table is at ingress, hits the ingress, and missed at egress

	   meta.finish=1;
	   meta.hit_status=EGRESS_MISS;
	   //We have to let the packet be sent to the speculate port or drop the packet if SwId is 0
	   if(meta.SwId == 0) {
	      drop();
	      return;
	   } else {
	      add_sw_id_to_packet();

	      //remove headers only if the packet is recirculated..
              //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	      if(meta.instance_type == RECIRC)
    	          remove_headers();

	      //let the packet be sent to the speculate port set at ingress
	   }
        }
      } else {
         //table_lookup==0

         //remove headers only if the packet is recirculated..
         //otherwise, there is no reason to call remove_headers since the headers was not added to the packet
    	 if(meta.instance_type == RECIRC)
    	     remove_headers();
      }
    }  // end apply
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control MyComputeChecksum(inout headers  hdr, inout metadata meta) {
     apply {
        update_checksum(
        hdr.ipv4.isValid(),
            { hdr.ipv4.version,
              hdr.ipv4.ihl,
              hdr.ipv4.diffserv,
              hdr.ipv4.totalLen,
              hdr.ipv4.identification,
              hdr.ipv4.flags,
              hdr.ipv4.fragOffset,
              hdr.ipv4.ttl,
              hdr.ipv4.protocol,
              hdr.ipv4.srcAddr,
              hdr.ipv4.dstAddr },
            hdr.ipv4.hdrChecksum,
            HashAlgorithm.csum16);
    }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control MyDeparser(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.number_passes);
        packet.emit(hdr.number_elements);
        packet.emit(hdr.elements);
        packet.emit(hdr.number_elements_copy);
        packet.emit(hdr.elements_copy);
        packet.emit(hdr.ipv4);
    }
}

/*************************************************************************
***********************  S W I T C H  *******************************
*************************************************************************/

V1Switch(
   MyParser(),
   MyVerifyChecksum(),
   MyIngress(),
   MyEgress(),
   MyComputeChecksum(),
   MyDeparser()
) main;
