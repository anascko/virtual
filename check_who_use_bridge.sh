#!/bin/bash
#your_bridge=$1
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"
#check hostnname and eth2 interface name
case $(hostname) in
cz7918)
eth=p4p2;;
cz7764.bud.mirantis.net)
eth=eth1;;
cz7364.bud.mirantis.net)
eth=em2;;
cz7363.bud.mirantis.net)
eth=em2;;
cz5578)
eth=em2;;
srv78-bud)
eth=p4p2;;
esac
VNET=`(brctl show | grep "$eth"| awk '{print $1}')`
if [[ $VNET == "" ]]
    then
    echo "Interface $eth is free and will be configured with your intarface aded in params $your_bridge"
    	if [[ $your_bridge == "0" ]]
    	then echo "You incorrectly specified parameter $your_bridge interface"
    	exit
	fi
    stp_check=`(brctl show | grep "$your_bridge" | awk '{print $3}')`
    echo $stp_check
        if [[ $stp_check == "yes" ]]
	then
	    brctl stp $your_bridge no
	fi
    brctl addif $your_bridge $eth
    exit 1;
fi
# check if exist
#else
    VNET1=`(echo $VNET | cut -c1-4)`
    name_vm=""
    if [[ $VNET1 == "fuel" ]]
	then
	for vm in $(virsh list --name)
        do
	    name_vm=`virsh dumpxml $vm|grep -q "$VNET" && echo $vm`
        done

    else
#it's devsteck
     adr_node=`(ifconfig $VNET | awk '/addr:/ {print  $2}' |cut -c6-21 | cut -d. -f 1,2,3)`
#     echo $adr_node
     ping_exist=`(ping -c 1 $adr_node.1 | head -2 | awk '/bytes from/ {print $1}')`
#echo $mac
	if [[ $ping_exist == '64' ]]
      	then
    	    VNET_mac=`(sshpass -p r00tme ssh -o "StrictHostKeyChecking no" root@$adr_node.1 'ifconfig eth1 | grep HWaddr' | awk '{print $5}')`
	    mac_vm=""        	
	    for vm in $(virsh list --name)
		do
        	mac_vm=`virsh dumpxml $vm|grep "$VNET_mac"`
              	    if [[ $mac_vm != "" ]]
            		then 
                	name_vm=$vm
	      	    fi 
           	done
#        echo $name_vm
  	else
    	    exit 1
    	fi
    
fi
# adr_node=`(ifconfig $VNET | awk '/addr:/ {print  $2}' |cut -c6-21)
echo "Please ask's admin of $name_vm turn off bridge $VNET from $eth"
