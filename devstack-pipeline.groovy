/**
 * Create nodes 
 *
 * Expected parameters:
 *   DEVSTACK_REPO
 *   DEVSTACK_BRANCH
 *   LOCAL_CONF
**/

def common = new com.mirantis.mk.Common()
def git = new com.mirantis.mk.Git()


def CreateVM(vm_name, kvm_soket, img){
    return sh(script:"""
    export new_img=${vm_name} &&
    export LIBVIRT_SOCKET=${kvm_soket:-"--connect=qemu:///system"} && 
    export old_img=${img:-devstack-generic-ubuntu-xenial} &&
    virt-clone ${LIBVIRT_SOCKET} -o ${old_img} -n ${new_img} --auto-clone &&
    virsh start ${new_img} &&
    mac=$(virsh domiflist ${new_img} | awk '/network/ {print $5}') &&
    echo ${mac} &&
    ip=$(/usr/sbin/arp -an  |grep "${mac}" | grep -o -P '(?<=\? \().*(?=\) .*)') &&
    echo ${ip}
    """, returnStdout: true)
}

def DeployVM(envip, repos, branch, conf){
    return sh(script:"""
    export ENV_IP=${envip} &&
    export DEVSTACK_REPO=${repos} &&
    export DEVSTACK_BRANCH=${branch} &&
    export LOCAL_CONF=${conf} &&
    echo "$LOCAL_CONF"  > /tmp/local.conf 
    """, returnStdout: true)                                                                              } 


node() {
    LIBVIRT_SOCKET = "--connect=qemu:///system"
    DISTRO_RELEASE = 'devstack-generic-ubuntu-xenial'
    SSHUSER = root
    SSHPASS = r00tme
    SSHPORT = 22
    
    stage ('Create VM') {

    try {
        ENV_IP = CreateVM("${params.ENV_NAME}","${LIBVIRT_SOCKET}","${params.DISTRO_RELEASE}").trim()
        echo "${ENV_IP}"
    }
    }
    
    stage (Deploy Devstack){
      writeFile file: '/tmp/ssh-config', text: """\
        StrictHostKeyChecking no
        UserKnownHostsFile /dev/null
        ForwardAgent yes
        User ${SSHUSER}
        Port ${SSHPORT}
    """.stripIndent()

      writeFile file: '/tmp/local.conf', text: "${LOCAL_CONF}"

      sh "sshpass -e ssh -F \"/tmp/ssh-config\" -p r00tme ${ENV_IP} useradd -s /bin/bash -d /opt/stack -m stack &&
     sed -i "s/devstack-generic/$ENV_NAME/g" /etc/hosts && 
     hostname $ENV_NAME && 
     echo "stack:r00tme" |  chpasswd"
      sh "sshpass -e ssh stack@${ENV_IP} -p r00tme git clone $DEVSTACK_REPO -b DEVSTACK_BRANCH /opt/stack/devstack"
        sh "sshpass -e ssh stack@${ENV_IP} -p r00tme cd ~/devstack; ./stack.sh; exit"
        }
}
}
